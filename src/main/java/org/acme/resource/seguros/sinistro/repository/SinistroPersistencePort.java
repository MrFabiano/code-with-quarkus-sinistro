package org.acme.resource.seguros.sinistro.repository;

import jakarta.transaction.Transactional;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.Sinistro;

import java.util.Optional;

public interface SinistroPersistencePort {
    Sinistro salvar(Sinistro sinistro);
    Optional<Sinistro> buscarPorUuid(String uuid);
    Sinistro atualizar(Sinistro sinistro);
}
