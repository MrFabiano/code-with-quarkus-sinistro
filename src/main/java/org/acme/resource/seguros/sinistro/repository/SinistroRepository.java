package org.acme.resource.seguros.sinistro.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.EventoPendente;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.Sinistro;

@ApplicationScoped
public class SinistroRepository implements PanacheRepositoryBase<Sinistro, Long> {
    // Métodos específicos podem ser adicionados aqui
}