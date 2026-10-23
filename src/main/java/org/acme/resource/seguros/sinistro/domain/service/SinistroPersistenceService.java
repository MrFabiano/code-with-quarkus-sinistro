package org.acme.resource.seguros.sinistro.domain.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.Sinistro;
import org.acme.resource.seguros.sinistro.domain.SinistroStatus;

import java.util.UUID;

@ApplicationScoped
public class SinistroPersistenceService {

    @Transactional
    public Sinistro persistir(Sinistro sinistro) {
        sinistro.uuid = UUID.randomUUID().toString();
        sinistro.status = SinistroStatus.ABERTO;
        sinistro.persist();
        return sinistro;
    }
}