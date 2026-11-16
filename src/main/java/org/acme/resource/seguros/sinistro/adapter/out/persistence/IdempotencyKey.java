package org.acme.resource.seguros.sinistro.adapter.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_key")
public class IdempotencyKey extends PanacheEntityBase {

    @Id
    @Column(length = 100)
    public String chave;

    @Column(name = "hash_requisicao", nullable = false, length = 64)
    public String hashRequisicao;

    @Column(name = "resultado_tipo", length = 20)
    public String resultadoTipo;

    @Column(name = "resposta_json", columnDefinition = "text")
    public String respostaJson;

    @Column(name = "criado_em", nullable = false)
    public LocalDateTime criadoEm;
}