package org.acme.resource.seguros.sinistro.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.acme.resource.seguros.sinistro.adapter.in.rest.CamelRouterClient;
import org.acme.resource.seguros.sinistro.adapter.in.rest.dto.ContingenciaResponseDTO;
import org.acme.resource.seguros.sinistro.adapter.in.rest.dto.SinistroPayload;
import org.acme.resource.seguros.sinistro.domain.SinistroStatus;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static io.smallrye.jwt.config.ConfigLogging.log;


@ApplicationScoped
public class SinistroService {
    private static final Logger LOG = LoggerFactory.getLogger(SinistroService.class);

    @Inject
    SinistroProducer producer;

    @Inject
    @RestClient
    CamelRouterClient camelRouterClient;

    @Inject
    MeterRegistry meterRegistry;

    @Inject
    ContingenciaLocalService contingenciaLocalService;

    @Inject
    SinistroPersistence persistence;

    private Counter counterValidacoesBlok;
    private Counter counterSucesso;
    private Counter counterFallback;

    void onStart(@Observes StartupEvent ev) {
        LOG.info("|| BUILD MARKER || versao=2026-10-22-20h30");
    }

    @PostConstruct
    public void init() {
        LOG.info("|| INIT counters ||");
        counterValidacoesBlok = Counter.builder("sinistro_validacoes_bloqueadas_total").register(meterRegistry);
        counterSucesso = Counter.builder("sinistro_sucesso_total").register(meterRegistry);
        counterFallback = Counter.builder("sinistro_fallback_total").register(meterRegistry);
    }

    @Retry(maxRetries = 2, delay = 500)
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 5000)
    @Fallback(fallbackMethod = "tratarContingencia")
    public Response executarProcessamento(Sinistro sinistro) throws JsonProcessingException {
        LOG.info("|| SERVICE RECEBEU || apoliceId={}", sinistro.apoliceId);

        // Validações de payload (Retornam 400 se falhar)
        if (sinistro.apoliceId == null || sinistro.apoliceId.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new FriendlyResponse("Erro de Validação", "O campo 'apoliceId' é obrigatório."))
                    .build();
        }

        if (sinistro.valorEstimado == null || sinistro.valorEstimado.compareTo(BigDecimal.ZERO) <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new FriendlyResponse("Erro de Validação", "O campo 'valorEstimado' deve ser maior que zero."))
                    .build();
        }

        if (sinistro.descricao == null || sinistro.descricao.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new FriendlyResponse("Erro de Validação", "O campo 'descricao' é obrigatório."))
                    .build();
        }

        // Persistência local (Grava no Postgres antes do Kafka)
        persistence.salvar(sinistro);

        // Tenta publicar no Kafka. Se falhar, estoura a exceção para acionar @Retry e @Fallback
        producer.publicar(toSinistroEvent(sinistro));

        counterSucesso.increment();
        return Response.status(201).entity(sinistro).build();
    }

    // Método de Fallback invocado automaticamente quando o Retry/CircuitBreaker esgota
    public Response tratarContingencia(Sinistro sinistro, Throwable causa) {
        log.warnf("!!! FALLBACK ACIONADO !!! Motivo: %s", causa != null ? causa.getMessage() : "Desconhecido");

        String motivoFalha = "Kafka e Camel Router indisponíveis: " + (causa != null ? causa.getMessage() : "Erro desconhecido");

        // 1. Tenta acionar o Camel Router via REST Client
        try {
            ContingenciaResponseDTO response = camelRouterClient.enviarContingencia(toSinistroPayload(sinistro));
            if (response != null && "processed".equalsIgnoreCase(response.status())) {
                return Response.status(Response.Status.ACCEPTED)
                        .entity(new FriendlyResponse("Contingência", "Processado via Camel Router"))
                        .build();
            }
        } catch (Exception e) {
            LOG.error("Camel Router indisponível. Gravando contingência localmente...", e);
        }

        // 2. Grava localmente no Postgres (tabela evento_pendente)
        try {
            contingenciaLocalService.gravarContingencia(sinistro, motivoFalha);
        } catch (Exception e) {
            LOG.error("Falha ao gravar contingência local", e);
        }

        // 3. Retorna 202 Accepted explicitamente sem deixar nenhuma exceção subir
        return Response.status(Response.Status.ACCEPTED)
                .entity(new FriendlyResponse("Contingência Local", "Evento registrado com sucesso na fila de pendências."))
                .build();
    }
    private SinistroEvent toSinistroEvent(Sinistro sinistro) {
        return new SinistroEvent(
                sinistro.uuid,
                "SINISTRO_CRIADO",
                sinistro.apoliceId,
                sinistro.descricao,
                sinistro.valorEstimado,
                sinistro.status,
                LocalDateTime.now()
        );
    }

    private SinistroPayload toSinistroPayload(Sinistro sinistro) {
        return new SinistroPayload(sinistro.uuid, sinistro.apoliceId, sinistro.valorEstimado);
    }
}