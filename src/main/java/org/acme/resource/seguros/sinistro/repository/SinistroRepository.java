package org.acme.resource.seguros.sinistro.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.EventoPendente;

@ApplicationScoped
public class ContingenciaRepository {

    @Transactional
    public void remover(Long id) {
        EventoPendente.deleteById(id);
    }
}