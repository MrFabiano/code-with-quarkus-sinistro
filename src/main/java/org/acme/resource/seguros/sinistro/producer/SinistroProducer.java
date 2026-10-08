package org.acme.resource.seguros.sinistro.producer;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.acme.resource.seguros.sinistro.adapter.in.rest.dto.SinistroPayload;
import org.acme.resource.seguros.sinistro.domain.model.SinistroEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.util.UUID;

@ApplicationScoped
public class SinistroProducer {

    // Variável para simular falhas (você pode alternar isso manualmente)
    @ConfigProperty(name = "app.simular-falha", defaultValue = "false")
    boolean simularFalha;

    @Inject
    @Channel("seguros-sinistros-out")
    Emitter<SinistroEvent> emitter;
    public void publicar(SinistroEvent evento) {
        System.out.println("--- [PRODUCER] simularFalha = " + simularFalha);

        // 1. SIMULAÇÃO DE FALHA (Dispara o @Retry e o @Fallback do Service)
        if (simularFalha) {
            System.err.println("--- [PRODUCER] Simulando falha de conexão com Kafka ---");
            throw new WebApplicationException("Kafka indisponível (Simulação)", 503);
        }
        // VALIDAÇÃO FINAL ANTES DO KAFKA - ÚLTIMA LINHA DE DEFESA
        // Última linha de defesa
        if (evento.apoliceId() == null || evento.apoliceId().isEmpty()) {
            throw new IllegalArgumentException("Não pode publicar evento inválido");
        }
        if (evento.valorEstimado() == null || evento.valorEstimado() <= 0) {
            throw new IllegalArgumentException("Não pode publicar evento inválido");
        }

        emitter.send(evento)
                .toCompletableFuture()
                .join();
    }
}