package org.acme.resource.seguros.sinistro.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.resource.seguros.sinistro.adapter.in.rest.CamelRouterClient;
import org.acme.resource.seguros.sinistro.adapter.in.rest.dto.ContingenciaResponseDTO;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.IdempotencyKey;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.ResultadoIdempotente;
import org.acme.resource.seguros.sinistro.api.request.SinistroRequest;
import org.acme.resource.seguros.sinistro.api.request.SinistroUpdateRequest;
import org.acme.resource.seguros.sinistro.api.response.SinistroResponse;
import org.acme.resource.seguros.sinistro.domain.SinistroStatus;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.Sinistro;
import org.acme.resource.seguros.sinistro.exception.*;
import org.acme.resource.seguros.sinistro.domain.mapper.SinistroMapper;
import org.acme.resource.seguros.sinistro.producer.SinistroProducer;
import org.acme.resource.seguros.sinistro.repository.SinistroPersistencePort;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.LocalDateTime;
import java.util.List;

import static io.smallrye.jwt.config.ConfigLogging.log;

@ApplicationScoped
public class SinistroService {

    private static final Logger LOG = LoggerFactory.getLogger(SinistroService.class);

    @Inject
    SinistroMapper mapper;
    @Inject
    SinistroProducer producer;
    @Inject
    ContingenciaLocalService contingenciaLocalService;
    @Inject
    MeterRegistry meterRegistry;
    @Inject
    SinistroPersistencePort persistencePort;
    @Inject
    IdempotenciaService idem;
    @Inject
    ObjectMapper json;
    @Inject
    SecurityIdentity identity;

    @Inject
    @RestClient
    CamelRouterClient camelRouterClient;

    private Counter counterSucesso;
    private Counter counterFallback;

    @PostConstruct
    void init() {
        counterSucesso = Counter.builder("sinistro_sucesso_total").register(meterRegistry);
        counterFallback = Counter.builder("sinistro_fallback_total").register(meterRegistry);
    }

    // ---------------------------------------------------------------
    // CRIAÇÃO
    // ---------------------------------------------------------------
    public ResultadoCriacao criar(SinistroRequest request) {
        log.infof("|| SERVICE RECEBEU || apoliceId=%s", request.apoliceId());

        Sinistro sinistro = mapper.toEntity(request);
        try {
            persistencePort.salvar(sinistro);
        } catch (Exception e) {
            LOG.error("Falha ao persistir sinistro", e);
            throw new ContingenciaIndisponivelException(
                    "Não foi possível registrar o sinistro. Tente novamente.", e);
        }

        try {
            producer.publicar(mapper.toEvent(sinistro));
            counterSucesso.increment();
            return new ResultadoCriacao.Criado(mapper.toResponse(sinistro));
        } catch (Exception e) {
            return tratarContingencia(sinistro, e);
        }
    }

    // ---------------------------------------------------------------
    // CONTINGÊNCIA: Camel Router -> banco local
    // ---------------------------------------------------------------
    private ResultadoCriacao tratarContingencia(Sinistro sinistro, Throwable causa) {
        counterFallback.increment();
        String causaMsg = causa != null ? causa.getMessage() : "Erro desconhecido";
        log.warnf("!!! FALLBACK ACIONADO !!! Motivo: %s", causaMsg);

        // 1. Tenta o Camel Router
        try {
            ContingenciaResponseDTO resp =
                    camelRouterClient.enviarContingencia(mapper.toPayload(sinistro));

            if (resp != null && "processed".equalsIgnoreCase(resp.status())) {
                return new ResultadoCriacao.EmContingencia("Processado via Camel Router");
            }
        } catch (Exception e) {
            // NÃO lança: segue para a gravação local
            LOG.error("Camel Router indisponível. Gravando contingência localmente...", e);
        }

        // 2. Grava localmente em EventoPendente (o scheduler reprocessa depois)
        String motivoFalha = "Kafka e Camel Router indisponíveis: " + causaMsg;
        try {
            contingenciaLocalService.gravarContingencia(sinistro, motivoFalha);
        } catch (Exception e) {
            // Só aqui o sinistro existe e nada registrou o evento
            LOG.error("Falha ao gravar contingência local. sinistro={}", sinistro.uuid, e);
            throw new SinistroSemEventoException(sinistro.uuid, e);
        }

        return new ResultadoCriacao.EmContingencia(
                "Evento registrado com sucesso na fila de pendências.");
    }

    // ---------------------------------------------------------------
    // CONSULTAS
    // ---------------------------------------------------------------
    public List<SinistroResponse> listarTodos() {
        return mapper.toResponseList(Sinistro.listAll());
    }

    public SinistroResponse buscarPorUuid(String uuid) {
        return mapper.toResponse(buscarEntidade(uuid));
    }

    // ---------------------------------------------------------------
    // ATUALIZAÇÃO
    // ---------------------------------------------------------------
    @Transactional
    public SinistroResponse atualizar(String uuid, SinistroUpdateRequest request) {
        Sinistro sinistro = buscarEntidade(uuid);
        if (sinistro.status.isFinal()) {
            throw new SinistroJaFechadoException();
        }
        aplicarTransicao(sinistro, request.status());
        mapper.updateEntity(request, sinistro);
        return mapper.toResponse(sinistro);
    }

    private void aplicarTransicao(Sinistro sinistro, SinistroStatus novo) {
        if (novo == null || novo == sinistro.status) {
            return;
        }
        if (!sinistro.status.podeIrPara(novo)) {
            throw new TransicaoInvalidaException(sinistro.status, novo);
        }
        sinistro.status = novo;
        if (novo == SinistroStatus.FECHADO) {
            sinistro.dataConclusao = LocalDateTime.now();
        }
    }

    @Transactional
    public SinistroResponse alterarStatus(String uuid, SinistroStatus novo) {
        Sinistro sinistro = buscarEntidade(uuid);
        if (sinistro.status.isFinal()) {
            throw new SinistroJaFechadoException();
        }
        aplicarTransicao(sinistro, novo);
        return mapper.toResponse(sinistro);
    }

    // ---------------------------------------------------------------
    // EXCLUSÃO
    // ---------------------------------------------------------------
    @Transactional
    public void deletar(String uuid) {
        buscarEntidade(uuid).delete();
    }

    // ---------------------------------------------------------------
    private Sinistro buscarEntidade(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            throw new IllegalArgumentException("UUID não pode ser vazio");
        }
        Sinistro sinistro = Sinistro.find("uuid", uuid).firstResult();
        if (sinistro == null) {
            throw new SinistroNaoEncontradoException(uuid);
        }
        return sinistro;
    }

    public ResultadoIdempotente criarIdempotente(SinistroRequest request,
                                                 String chaveCliente) {
        if (chaveCliente == null || chaveCliente.isBlank()) {
            return new ResultadoIdempotente(criar(request), false);   // comportamento atual
        }
        String chave = solicitante() + ":" + chaveCliente;               // escopo por usuário
        String hash = IdempotenciaService.sha256(escrever(request));

        if (idem.reservar(chave, hash)) {
            try {
                ResultadoCriacao r = criar(request);
                idem.concluir(chave, tipoDe(r), escrever(r));
                return new ResultadoIdempotente(r, false);
            } catch (SinistroSemEventoException e) {
                idem.concluir(chave, "SEM_EVENTO", e.getUuid());   // o sinistro existe: NÃO liberar a chave
                throw e;
            } catch (RuntimeException e) {
                idem.liberar(chave);                               // nada foi gravado: pode repetir
                throw e;
            }
        }

        IdempotencyKey k = idem.buscar(chave).orElseThrow();
        if (!k.hashRequisicao.equals(hash)) {
            throw new ChaveIdempotenciaReusadaException();
        }
        if (k.resultadoTipo == null) {
            throw new RequisicaoEmProcessamentoException();
        }
        if ("SEM_EVENTO".equals(k.resultadoTipo)) {
            throw new SinistroSemEventoException(k.respostaJson, null);   // repetição: mesmo 503, sem duplicar
        }
        return new ResultadoIdempotente(reconstruir(k), true);
    }

    private String tipoDe(ResultadoCriacao r) {
        return switch (r) {
            case ResultadoCriacao.Criado c -> "CRIADO";
            case ResultadoCriacao.EmContingencia c -> "CONTINGENCIA";
        };
    }

    private ResultadoCriacao reconstruir(IdempotencyKey k) {
        try {
            return switch (k.resultadoTipo) {
                case "CRIADO" -> json.readValue(k.respostaJson, ResultadoCriacao.Criado.class);
                default -> json.readValue(k.respostaJson, ResultadoCriacao.EmContingencia.class);
            };
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private String escrever(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private String solicitante() {
        if (identity.isAnonymous()) {
            return "anonimo";                              // testes e chamadas sem token
        }
        if (identity.getPrincipal() instanceof JsonWebToken token && token.getSubject() != null) {
            return token.getSubject();                     // produção: o "sub" do Keycloak
        }
        return identity.getPrincipal().getName();
    }
}