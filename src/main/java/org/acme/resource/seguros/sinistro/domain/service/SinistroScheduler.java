package org.acme.resource.seguros.sinistro.domain.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.panache.common.Page;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.EventoPendente;
import org.acme.resource.seguros.sinistro.domain.model.SinistroEvent;
import org.acme.resource.seguros.sinistro.infrastructure.config.SinistroEventMapper;
import org.acme.resource.seguros.sinistro.producer.SinistroProducer;
import org.acme.resource.seguros.sinistro.repository.ContingenciaRepository;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;


import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class SinistroScheduler {

    private static final Logger LOG = Logger.getLogger(SinistroScheduler.class);

    @Inject
    SinistroEventMapper mapper;

    @Inject
    SinistroProducer producer;

    @Inject
    EventoPendenteService eventoPendenteService;

    @Inject
    @Channel("seguros-sinistros-dlq")
    Emitter<String> dlqEmitter;

    @Scheduled(every = "1m")
    public void reprocessarEventosPendentes() {
        List<EventoPendente> pendentes =
                EventoPendente.list("status = ?1 order by id", "PENDENTE");

        if (pendentes.isEmpty()) {
            return;
        }

        LOG.infof("|| SCHEDULER || Iniciando reprocessamento de %d eventos...", pendentes.size());

        for (EventoPendente evento : pendentes) {
            processarUm(evento);
        }
    }

    private void processarUm(EventoPendente evento) {
        SinistroEvent sinEvent;

        // ---- Etapa 1: deserializar + validar (falha = permanente) ----
        try {
            sinEvent = mapper.fromJson(evento.payloadJson);
            validarEvento(sinEvent);
        } catch (Exception e) {
            rejeitar(evento, e.getMessage());
            return;
        }

        // ---- Etapa 2: publicar (falha = transitória) ----
        try {
            producer.publicar(sinEvent);
            eventoPendenteService.remover(evento.id);
            LOG.infof("|| SCHEDULER || Evento ID %d reenviado e removido.", evento.id);

        } catch (Exception e) {
            LOG.warnf("|| SCHEDULER || Evento ID %d ainda pendente (transitório): %s",
                    evento.id, e.getMessage());
            // não altera o status: continua PENDENTE para a próxima execução
        }
    }

    private void rejeitar(EventoPendente evento, String razao) {
        String motivo = "Payload inválido: " + razao;
        if (motivo.length() > 240) {
            motivo = motivo.substring(0, 237) + "...";
        }

        eventoPendenteService.marcarComoRejeitado(evento.id, motivo);
        enviarParaDLQ(evento, motivo);

        LOG.errorf("|| SCHEDULER || Evento ID %d REJEITADO (erro permanente): %s",
                evento.id, razao);
    }

    // ---- Os dois métodos que estavam vazios ----

    private void validarEvento(SinistroEvent evento) {
        if (evento.apoliceId() == null || evento.apoliceId().isBlank()) {
            throw new IllegalArgumentException("apoliceId é obrigatório");
        }
        if (evento.valorEstimado() == null
                || evento.valorEstimado().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("valorEstimado deve ser maior que zero");
        }
        if (evento.descricao() == null || evento.descricao().isBlank()) {
            throw new IllegalArgumentException("descricao é obrigatória");
        }
    }

    private void enviarParaDLQ(EventoPendente evento, String razao) {
        try {
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("id", evento.id);
            envelope.put("payloadOriginal", evento.payloadJson);
            envelope.put("motivoOriginal", evento.motivoFalha);
            envelope.put("motivoRejeicao", razao);
            envelope.put("dataRejeicao", LocalDateTime.now().toString());

            dlqEmitter.send(mapper.toJson(envelope))
                    .toCompletableFuture()
                    .join();

            LOG.infof("|| DLQ || Evento ID %d enviado para seguros-sinistros-dlq", evento.id);

        } catch (Exception e) {
            LOG.errorf("|| DLQ || Falha ao enviar Evento ID %d: %s",
                    evento.id, e.getMessage());
            // o registro já está REJEITADO no banco; a DLQ é complementar
        }
    }
}