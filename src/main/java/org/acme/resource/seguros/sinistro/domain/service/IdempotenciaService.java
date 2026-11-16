package org.acme.resource.seguros.sinistro.domain.service;

import io.quarkus.hibernate.orm.panache.Panache;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.IdempotencyKey;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@ApplicationScoped
public class IdempotenciaService {

    @Inject
    EntityManager em;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public boolean reservar(String chave, String hash) {
        int n = em.createNativeQuery(
                        "insert into idempotency_key (chave, hash_requisicao, criado_em) "
                                + "values (?1, ?2, now()) on conflict (chave) do nothing")
                .setParameter(1, chave)
                .setParameter(2, hash)
                .executeUpdate();
        return n == 1;                       // false: a chave já existia
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void concluir(String chave, String tipo, String respostaJson) {
        em.createQuery("update IdempotencyKey k set k.resultadoTipo = ?1, k.respostaJson = ?2 "
                        + "where k.chave = ?3")
                .setParameter(1, tipo)
                .setParameter(2, respostaJson)
                .setParameter(3, chave)
                .executeUpdate();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void liberar(String chave) {
        em.createQuery("delete from IdempotencyKey k where k.chave = ?1")
                .setParameter(1, chave)
                .executeUpdate();
    }

    @Transactional
    public Optional<IdempotencyKey> buscar(String chave) {
        return Optional.ofNullable(em.find(IdempotencyKey.class, chave));
    }

    public static String sha256(String texto) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256")
                    .digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}