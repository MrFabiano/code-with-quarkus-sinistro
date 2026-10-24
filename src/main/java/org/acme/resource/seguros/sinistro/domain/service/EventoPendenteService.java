package org.acme.resource.seguros.sinistro.domain.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.EventoPendente;

import java.time.LocalDateTime;

@ApplicationScoped
public class EventoPendenteService {

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void remover(Long id) {
        EventoPendente.deleteById(id);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void marcarComoRejeitado(Long id, String motivo) {
        EventoPendente e = EventoPendente.findById(id);
        if (e == null) {
            return;
        }
        e.status = "REJEITADO";
        e.motivoFalha = motivo;
        e.dataRejeicao = LocalDateTime.now();
        e.processado = false;
        e.persist();
    }
}

