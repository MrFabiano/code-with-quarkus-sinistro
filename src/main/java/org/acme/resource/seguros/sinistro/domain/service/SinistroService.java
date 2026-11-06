package org.acme.resource.seguros.sinistro.domain.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.resource.seguros.sinistro.adapter.in.rest.CamelRouterClient;
import org.acme.resource.seguros.sinistro.adapter.in.rest.dto.ContingenciaResponseDTO;
import org.acme.resource.seguros.sinistro.api.request.SinistroRequest;
import org.acme.resource.seguros.sinistro.api.request.SinistroUpdateRequest;
import org.acme.resource.seguros.sinistro.api.response.SinistroResponse;
import org.acme.resource.seguros.sinistro.domain.SinistroStatus;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.Sinistro;
import org.acme.resource.seguros.sinistro.exception.ContingenciaIndisponivelException;
import org.acme.resource.seguros.sinistro.exception.SinistroJaFechadoException;
import org.acme.resource.seguros.sinistro.exception.SinistroNaoEncontradoException;
import org.acme.resource.seguros.sinistro.domain.mapper.SinistroMapper;
import org.acme.resource.seguros.sinistro.exception.TransicaoInvalidaException;
import org.acme.resource.seguros.sinistro.producer.SinistroProducer;
import org.acme.resource.seguros.sinistro.repository.SinistroPersistencePort;
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

    @Inject SinistroMapper mapper;
    @Inject SinistroProducer producer;
    @Inject ContingenciaLocalService contingenciaLocalService;
    @Inject MeterRegistry meterRegistry;
    @Inject SinistroPersistencePort persistencePort;

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
            LOG.error("Camel Router indisponível. Gravando contingência localmente...", e);
        }

        // 2. Grava localmente em EventoPendente (o scheduler reprocessa depois)
        String motivoFalha = "Kafka e Camel Router indisponíveis: " + causaMsg;
        try {
            contingenciaLocalService.gravarContingencia(sinistro, motivoFalha);
        } catch (Exception e) {
            LOG.error("Falha ao gravar contingência local", e);
            throw new ContingenciaIndisponivelException(
                    "Não foi possível registrar o evento.", e);
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
        if (novo != sinistro.status) {
            if (!sinistro.status.podeIrPara(novo)) {
                throw new TransicaoInvalidaException(sinistro.status, novo);
            }
            sinistro.status = novo;
            if (novo == SinistroStatus.FECHADO) {
                sinistro.dataConclusao = LocalDateTime.now();
            }
        }
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
}