package org.acme.resource.seguros.sinistro.domain.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.panache.common.Page;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.EventoPendente;
import org.acme.resource.seguros.sinistro.domain.model.SinistroEvent;
import org.acme.resource.seguros.sinistro.producer.SinistroProducer;
import org.acme.resource.seguros.sinistro.repository.ContingenciaRepository;
import org.jboss.logging.Logger;


import java.time.Duration;
import java.util.List;

@ApplicationScoped
public class SinistroScheduler {

    private static final Logger LOG = Logger.getLogger(SinistroScheduler.class);

    @Inject
    SinistroProducer producer;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ContingenciaRepository contingenciaRepository;

    @Scheduled(every = "1m")
    public void processarPendencias() {
        List<EventoPendente> pendentes = EventoPendente.findAll().page(Page.ofSize(100)).list();
        if (pendentes.isEmpty()) return;

        LOG.infof("|| SCHEDULER || Iniciando reprocessamento de %d eventos...", pendentes.size());

        for (EventoPendente p : pendentes) {
            Long id = p.id;
            String payload = p.payloadJson;

            SinistroEvent evento;
            try {
                evento = objectMapper.readValue(payload, SinistroEvent.class);
            } catch (Exception e) {
                LOG.errorf("|| SCHEDULER || Payload corrompido no ID %d: %s", id, e.getMessage());
                continue;
            }

            try {
                producer.publicar(evento);        // fora de transação
            } catch (Exception e) {
                LOG.errorf("|| SCHEDULER || Kafka fora, abortando lote: %s", e.getMessage());
                break;                            // não insiste nos outros 99
            }

            contingenciaRepository.remover(id);   // transação curta
            LOG.infof("|| SCHEDULER || Evento ID %d reenviado e removido.", id);
        }
    }
}