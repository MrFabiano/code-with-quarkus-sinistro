package org.acme.resource.seguros.sinistro.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.resource.seguros.sinistro.domain.model.SinistroEvent;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.Sinistro;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import java.util.UUID;
import java.util.concurrent.CompletionStage;


@ApplicationScoped
public class SinistroConsumer {

    private static final Logger LOG = Logger.getLogger(SinistroConsumer.class);
    @Inject // <--- Deixe o Quarkus gerenciar o ObjectMapper configurado
    ObjectMapper objectMapper;

    @Incoming("seguros-sinistros-in")
    @Blocking
    @Transactional
    public CompletionStage<Void> consumirEventos(Message<String> mensagem) {

        String payloadCru = mensagem.getPayload();
        try {
            // 1. Tenta fazer o parse do JSON. Se o JSON estiver corrompido/malformado, vai para o catch
            SinistroEvent evento = objectMapper.readValue(payloadCru, SinistroEvent.class);

            // 2. Validação de dados de negócio (Payload Inválido)
            if (evento.apoliceId() == null || evento.valorEstimado() == null || evento.valorEstimado() <= 0) {
                throw new IllegalArgumentException("Payload inválido: apoliceId ou valorEstimado ausentes/inválidos.");
            }
            // CHECAGEM DE IDEMPOTÊNCIA
//            UUID uuidEvento = (evento.uuid() instanceof UUID)
//                    ? (UUID) evento.uuid()
//                    : UUID.fromString(evento.uuid().toString());

            boolean jaExiste = Sinistro.count("uuid", evento.uuid()) > 0;
            if (jaExiste) {
                LOG.warnf("|| KAFKA CONSUMER || Evento descartado (já processado anteriormente). UUID: %s", evento.uuid());
                return mensagem.ack(); // Confirma o ack sem duplicar no banco
            }

            // 3. Processamento normal (Payload Válido)
            Sinistro sinistro = new Sinistro();
            sinistro.uuid = String.valueOf(evento.uuid());
            sinistro.apoliceId = evento.apoliceId();
            sinistro.descricao = evento.descricao();
            sinistro.valorEstimado = evento.valorEstimado();
            sinistro.status = evento.status();
            sinistro.persist();

            LOG.info("|| KAFKA CONSUMER || Sinistro processado e gravado no banco com sucesso!");
            return mensagem.ack(); // Confirma a leitura com sucesso

        } catch (Exception e) {
            // 4. Se houver erro de parse ou dados inválidos, envia para a DLQ
            LOG.error("|| DLQ ENVIADA || Falha ao processar mensagem. Enviando para a DLQ. Motivo: " + e.getMessage());
            return mensagem.nack(e); // Redireciona para o tópico seguros-sinistros-dlq
        }
    }
}
