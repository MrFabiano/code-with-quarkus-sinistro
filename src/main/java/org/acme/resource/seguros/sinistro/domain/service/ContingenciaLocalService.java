package org.acme.resource.seguros.sinistro.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.EventoPendente;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.Sinistro;
import org.acme.resource.seguros.sinistro.domain.model.SinistroEvent;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class ContingenciaLocalService {

    private static final Logger LOG = Logger.getLogger(ContingenciaLocalService.class);

    @Inject
    ObjectMapper objectMapper;   // ← faltava esta injeção no meu código anterior

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void gravarContingencia(Sinistro sinistro, String motivoFalha) {
        // idempotência: não duplica se já houver pendente para este uuid
        if (EventoPendente.count("uuid", sinistro.uuid) > 0) {
            LOG.warnf("|| CONTINGENCIA LOCAL || Já existe pendente para uuid %s", sinistro.uuid);
            return;
        }

        try {
            SinistroEvent evento = new SinistroEvent(
                    sinistro.uuid,
                    "SINISTRO_CONTINGENCIA",
                    sinistro.apoliceId,
                    sinistro.descricao,
                    sinistro.valorEstimado,
                    sinistro.status,
                    LocalDateTime.now()
            );

            EventoPendente pendente = new EventoPendente();
            pendente.uuid = sinistro.uuid;                        // <-- faltava
            pendente.payloadJson = objectMapper.writeValueAsString(evento);
            pendente.dataCriacao = LocalDateTime.now();
            pendente.motivoFalha = truncar(motivoFalha, 255);     // <-- evita estouro da coluna
            pendente.processado = false;
            pendente.status = "PENDENTE";
            pendente.persist();

            LOG.infof("|| CONTINGENCIA LOCAL || Evento gravado. ID: %d, uuid: %s",
                    pendente.id, sinistro.uuid);

        } catch (Exception e) {
            LOG.errorf(e, "|| CONTINGENCIA LOCAL || Falha ao gravar: %s", e.getMessage());
            throw new RuntimeException("Erro ao gravar contingência local", e);
        }
    }

    private static String truncar(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}