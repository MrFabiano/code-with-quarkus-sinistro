package org.acme.resource.seguros.sinistro.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

import java.util.UUID;

@Entity
public class Sinistro extends PanacheEntity {
    public UUID uuid = UUID.randomUUID();
    public String apoliceId;
    public String descricao;
    public Double valorEstimado;
    public String status; // ABERTO, EM_ANALISE, CONCLUIDO
}
