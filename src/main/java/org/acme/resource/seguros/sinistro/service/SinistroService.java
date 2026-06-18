package org.acme.resource.seguros.sinistro.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.acme.resource.seguros.sinistro.event.SinistroEvent;
import org.acme.resource.seguros.sinistro.model.DTO.MensagemResponse;
import org.acme.resource.seguros.sinistro.model.EventoPendente;
import org.acme.resource.seguros.sinistro.model.Sinistro;
import org.acme.resource.seguros.sinistro.producer.FriendlyResponse;
import org.acme.resource.seguros.sinistro.producer.SinistroProducer;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;


import java.time.LocalDateTime;

@ApplicationScoped
public class SinistroService {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    SinistroProducer producer;

    @Transactional
    @Retry(maxRetries = 3, delay = 1000)
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 5000)
    @Fallback(fallbackMethod = "fallbackCriarSinistro")
    public Response executarProcessamento(Sinistro sinistro) {
        sinistro.status = "ABERTO";
        sinistro.persist();

        var evento = new SinistroEvent(
                sinistro.uuid,
                "SINISTRO_CRIADO",
                sinistro.apoliceId,
                sinistro.descricao,
                sinistro.valorEstimado,
                sinistro.status,
                LocalDateTime.now()
        );

        // Se a propriedade estiver como true, isso vai estourar a exceção simulada
        producer.publicar(evento);

        return Response.status(201).entity(sinistro).build();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW) // <--- CRÍTICO: Abre uma nova transação isolada
    public Response fallbackCriarSinistro(Sinistro sinistro) throws JsonProcessingException {
        System.err.println("!!! FALLBACK ACIONADO: Kafka indisponível. Salvando no banco de dados...");

        // Cria o registro de segurança
        EventoPendente pendente = new EventoPendente();
        pendente.payloadJson = objectMapper.writeValueAsString(sinistro); // Ou converta para JSON real
        pendente.dataCriacao = LocalDateTime.now();
        pendente.motivoFalha = "Kafka Producer Unreachable";
        pendente.persist();

        return Response.status(Response.Status.ACCEPTED)
                .entity(new FriendlyResponse(
                        "Aviso",
                        "Sinistro registrado com sucesso, mas a notificação está pendente devido a instabilidade no sistema de eventos."
                ))
                .build();
    }
}
