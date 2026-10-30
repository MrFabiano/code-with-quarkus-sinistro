package org.acme.resource.seguros.sinistro.domain.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.EventoPendente;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

@ApplicationScoped
public class EventoPendenteService {

    @Transactional
    public List<Long> idsPendentes(int limite) {
        return EventoPendente.<EventoPendente>find("status = ?1 order by id", "PENDENTE")
                .page(0, limite).list()
                .stream().map(e -> e.id).toList();
    }

    /** Trava a linha (pulando as travadas por outras réplicas) e executa a ação na mesma transação. */
    @Transactional
    public void comLock(Long id, Consumer<EventoPendente> acao) {
        EventoPendente evento = (EventoPendente) EventoPendente.getEntityManager()
                .createNativeQuery("""
                        select * from eventopendente
                        where id = :id and status = 'PENDENTE'
                        for update skip locked
                        """, EventoPendente.class)
                .setParameter("id", id)
                .getResultStream().findFirst().orElse(null);

        if (evento == null) {
            return;   // outra réplica pegou ou já foi processado
        }
        acao.accept(evento);
    }
}
