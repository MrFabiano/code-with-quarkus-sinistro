package org.acme.resource.seguros.sinistro.service;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.acme.resource.seguros.sinistro.event.SinistroEvent;
import org.acme.resource.seguros.sinistro.model.Sinistro;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import org.jboss.logging.Logger;


@ApplicationScoped
public class SinistroConsumer {

    private static final Logger LOG = Logger.getLogger(SinistroConsumer.class);

    @Incoming("seguros-sinistros-in")
    @Transactional
    public void consumirEventos(SinistroEvent evento) {
        Sinistro sinistro = new Sinistro();
        // Se o evento trouxer um UUID usa ele, senão o mapeamento da classe gera o random
        if (evento.uuid() != null) {
            sinistro.uuid = evento.uuid();
        }
        sinistro.apoliceId = evento.apoliceId();
        sinistro.descricao = evento.descricao();
        sinistro.valorEstimado = evento.valorEstimado();
        sinistro.status = evento.status();

        sinistro.persist();

        Log.info("|| KAFKA CONSUMER || Sinistro gravado a partir do evento!");
        Log.infof("UUID do Sinistro: %s", sinistro.uuid);
        Log.infof("ID da Apólice: %s", sinistro.apoliceId);
        Log.infof("Descrição do Ocorrido: %s", sinistro.descricao);
        Log.infof("Valor Estimado: R$ %.2f", sinistro.valorEstimado);
        Log.infof("Status Atual: %s", sinistro.status);

    }
}