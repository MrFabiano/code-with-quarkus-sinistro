package org.acme.resource.seguros.sinistro.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.acme.resource.seguros.sinistro.adapter.in.rest.CamelRouterClient;
import org.acme.resource.seguros.sinistro.adapter.in.rest.dto.ContingenciaResponseDTO;
import org.acme.resource.seguros.sinistro.adapter.in.rest.dto.SinistroPayload;
import org.acme.resource.seguros.sinistro.domain.model.SinistroEvent;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.EventoPendente;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.Sinistro;
import org.acme.resource.seguros.sinistro.domain.port.output.FriendlyResponse;
import org.acme.resource.seguros.sinistro.producer.SinistroProducer;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import java.util.UUID;


@ApplicationScoped
public class SinistroService {
    private static final Logger LOG = LoggerFactory.getLogger(SinistroService.class);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    SinistroProducer producer;

    // REST Client para invocar o Camel Router na porta 8081
    @Inject
    @RestClient
    CamelRouterClient camelRouterClient;

    // Métricas
    @Inject
    MeterRegistry meterRegistry;

    // ============= MÉTRICAS (Adicione estas) =============

    private Counter counterValidacoesBlok;
    private Counter counterSucesso;
    private Counter counterFallback;

    @PostConstruct
    public void init() {
        counterValidacoesBlok = Counter.builder("sinistro_validacoes_bloqueadas_total")
                .description("Validações bloqueadas")
                .register(meterRegistry);

        counterSucesso = Counter.builder("sinistro_sucesso_total")
                .description("Sucessos")
                .register(meterRegistry);

        counterFallback = Counter.builder("sinistro_fallback_total")
                .description("Fallsbacks")
                .register(meterRegistry);
    }

    @Transactional
    @Retry(maxRetries = 3, delay = 1000)
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 5000)
    @Fallback(fallbackMethod = "fallbackCriarSinistro")
    public Response executarProcessamento(Sinistro sinistro) throws JsonProcessingException {
        LOG.info("|| SERVICE RECEBEU || apoliceId={}", sinistro.apoliceId);

        System.out.println("|| SERVICE RECEBEU || apoliceId=" + sinistro.apoliceId +
                ", valorEstimado=" + sinistro.valorEstimado);

        // Bloqueio 1: apoliceId
        if (sinistro.apoliceId == null || sinistro.apoliceId.trim().isEmpty()) {
            LOG.error("|| BLOCKED || apoliceId inválido");
            System.err.println("|| BLOCKED || apoliceId nulo ou vazio");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new FriendlyResponse("Erro de Validação",
                            "O campo 'apoliceId' é obrigatório e não pode ser nulo ou vazio."))
                    .build();
        }

        // Bloqueio 2: valorEstimado null
        if (sinistro.valorEstimado == null) {
            System.err.println("|| BLOCKED || valorEstimado nulo");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new FriendlyResponse("Erro de Validação",
                            "O campo 'valorEstimado' é obrigatório."))
                    .build();
        }

        // Bloqueio 3: valorEstimado negativo/zero
        if (sinistro.valorEstimado <= 0) {
            LOG.error("|| BLOCKED || valorEstimado inválido");
            System.err.println("|| BLOCKED || valorEstimado negativo ou zero: " + sinistro.valorEstimado);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new FriendlyResponse("Erro de Validação",
                            "O campo 'valorEstimado' deve ser maior que zero."))
                    .build();
        }

        // Bloqueio 4: descricao
        if (sinistro.descricao == null || sinistro.descricao.trim().isEmpty()) {
            System.err.println("|| BLOCKED || descricao nula ou vazia");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new FriendlyResponse("Erro de Validação",
                            "O campo 'descricao' é obrigatório."))
                    .build();
        }

        // ===========================================
        // TODAS AS VALIDAÇÕES PASSARAM
        // ===========================================
        LOG.info("|| VALIDADO OK || Iniciando persistência...");
        System.out.println("|| ALL VALIDATIONS PASSED || Iniciando persistência...");

        sinistro.status = "ABERTO";
        sinistro.persistAndFlush();

        System.out.println("### BANCO OK");

        var evento = new SinistroEvent(
                sinistro.uuid,
                "SINISTRO_CRIADO",
                sinistro.apoliceId,
                sinistro.descricao,
                sinistro.valorEstimado,
                sinistro.status,
                LocalDateTime.now()
        );

        LOG.info("|| SERVICE CHAMANDO PRODUCER ||");

        System.out.println("### EVENTO OK");

        producer.publicar(evento);

        LOG.info("|| SUCESSO NO KAFKA ||");
        counterSucesso.increment();
        return Response.status(201)
                .entity(sinistro)
                .build();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW) // <--- CRÍTICO: Abre uma nova transação isolada
    public Response fallbackCriarSinistro(Sinistro sinistro) throws JsonProcessingException {
        LOG.warn("!!! FALLBACK ACIONADO: Kafka indisponível! Redirecionando para o Camel Router...");
        counterFallback.increment();

        if (sinistro.uuid != null && Sinistro.count("uuid", sinistro.uuid) > 0) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new FriendlyResponse("Conflito", "Já existe um sinistro cadastrado com este UUID."))
                    .build();
        }

        // Monta o DTO esperado pelo Camel Router Client
        SinistroPayload payload = new SinistroPayload(
                sinistro.uuid,
                sinistro.apoliceId,
                sinistro.valorEstimado
        );

        try {
            // NÍVEL 2 DE CONTINGÊNCIA: Envia para o Camel Router (porta 8081)
            ContingenciaResponseDTO response = camelRouterClient.enviarContingencia(payload);
            LOG.info("Contingência processada com sucesso pelo Camel. Status: {}, RequestID: {}",
                    response.status(), response.requestId());

            return Response.status(Response.Status.ACCEPTED)
                    .entity(new FriendlyResponse(
                            "Aviso (Contingência Camel)",
                            "Sinistro aceito pelo Camel Router para processamento em background."
                    ))
                    .build();

        } catch (Exception e) {
            LOG.error("!!! ERRO CRÍTICO: Camel Router também falhou! Gravando contingência localmente...", e);

            // NÍVEL 3 DE CONTINGÊNCIA: Salva no banco de pendentes do próprio Quarkus-Sinistro
            EventoPendente pendente = new EventoPendente();
            pendente.payloadJson = objectMapper.writeValueAsString(sinistro);
            pendente.dataCriacao = LocalDateTime.now();
            pendente.motivoFalha = "Kafka e Camel Router indisponíveis: " + e.getMessage();
            pendente.persistAndFlush();

            return Response.status(Response.Status.ACCEPTED)
                    .entity(new FriendlyResponse(
                            "Aviso (Contingência Local)",
                            "Sinistro registrado no banco local de contingência devido a instabilidade geral nos serviços de integração."
                    ))
                    .build();
        }
    }
}