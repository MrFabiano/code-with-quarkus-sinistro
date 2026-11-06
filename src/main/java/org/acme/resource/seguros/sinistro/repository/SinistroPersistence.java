package org.acme.resource.seguros.sinistro.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.Sinistro;
import org.acme.resource.seguros.sinistro.domain.SinistroStatus;

import java.util.Optional;

@ApplicationScoped
public class SinistroPersistence implements SinistroPersistencePort {

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Sinistro salvar(Sinistro sinistro) {
        sinistro.status = SinistroStatus.ABERTO;
        sinistro.persistAndFlush();
        return sinistro;
    }

    @Override
    public Optional<Sinistro> buscarPorUuid(String uuid) {
        return Sinistro.find("uuid", uuid).firstResultOptional();
    }

    @Override
    public Sinistro atualizar(Sinistro sinistro) {
        sinistro.persistAndFlush();
        return sinistro;
    }
}