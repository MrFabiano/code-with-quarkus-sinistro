package org.acme.resource.seguros.sinistro.producer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.acme.resource.seguros.sinistro.event.SinistroEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

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
        if (simularFalha) {
            System.out.println("--- [PRODUCER] Simulando falha de conexão com Kafka ---");
            throw new WebApplicationException("Kafka indisponível!", 503);
        }
        emitter.send(evento);
    }
}