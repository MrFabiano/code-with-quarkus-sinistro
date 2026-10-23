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
        try {
            SinistroEvent evento = new SinistroEvent(
                    sinistro.uuid,
                    "SINISTRO_CONTINGENCIA",
                    sinistro.apoliceId,          // campo, sem ()
                    sinistro.descricao,
                    sinistro.valorEstimado,      // campo, sem ()
                    sinistro.status,
                    LocalDateTime.now()
            );

            EventoPendente pendente = new EventoPendente();
            pendente.payloadJson = objectMapper.writeValueAsString(evento);
            pendente.dataCriacao = LocalDateTime.now();
            pendente.motivoFalha = motivoFalha;
            pendente.processado = false;
            pendente.persist();

            LOG.infof("|| CONTINGENCIA LOCAL || Evento gravado. ID: %d, apoliceId: %s",
                    pendente.id, sinistro.apoliceId);

        } catch (Exception e) {
            LOG.errorf("|| CONTINGENCIA LOCAL || Falha ao gravar: %s", e.getMessage());
            throw new RuntimeException("Erro ao gravar contingência local", e);
        }
    }
}