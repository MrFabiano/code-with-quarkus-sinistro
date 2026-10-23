package org.acme.resource.seguros.sinistro.domain.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.Sinistro;
import org.acme.resource.seguros.sinistro.domain.SinistroStatus;

@ApplicationScoped
public class SinistroPersistence {

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Sinistro salvar(Sinistro sinistro) {
        sinistro.status = SinistroStatus.ABERTO;
        sinistro.persistAndFlush();
        return sinistro;
    }
}
