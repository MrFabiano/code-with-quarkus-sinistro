package org.acme.resource.seguros.sinistro.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.resource.seguros.sinistro.event.SinistroEvent;
import org.acme.resource.seguros.sinistro.model.EventoPendente;
import org.acme.resource.seguros.sinistro.producer.SinistroProducer;
import org.jboss.logging.Logger;

import java.util.List;
@ApplicationScoped
public class SinistroScheduler {

    @Inject
    Logger log;

    @Inject
    SinistroProducer producer;

    @Inject
    ObjectMapper objectMapper;

    @Scheduled(every = "1m")
    @Transactional
    public void processarPendencias() {
        // Paginação limitando a 100 registros por execução para controle de heap
        PanacheQuery<EventoPendente> query = EventoPendente.findAll().page(Page.ofSize(100));
        List<EventoPendente> pendentes = query.list();

        if (pendentes.isEmpty()) {
            return;
        }

        log.infof("|| SCHEDULER || Iniciando reprocessamento de %d eventos pendentes...", pendentes.size());

        for (EventoPendente pendencia : pendentes) {
            try {
                // Acesso direto ao atributo público payloadJson (Padrão Panache Active Record)
                SinistroEvent evento = objectMapper.readValue(pendencia.payloadJson, SinistroEvent.class);

                // Tenta enviar para o broker do Kafka
                producer.publicar(evento);

                // Remove do banco de dados em caso de sucesso
                pendencia.delete();

                log.infof("|| SCHEDULER || Evento ID %d reenviado e removido da contingencia.", pendencia.id);

            } catch (Exception e) {
                // Captura falhas individuais (ex: JSON corrompido ou Kafka ainda fora) sem quebrar o lote
                log.errorf("|| SCHEDULER || Falha ao reenviar evento ID %d. Motivo: %s", pendencia.id, e.getMessage());
            }
        }
    }
}