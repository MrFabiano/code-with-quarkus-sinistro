package org.acme.resource.seguros.sinistro.adapter.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.scheduler.Scheduled;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;

@Entity
public class EventoPendente extends PanacheEntity {

    @Column(nullable = false, unique = true)
    public String uuid;

    @Column(columnDefinition = "text")          // payload pode ser grande
    public String payloadJson;

    public LocalDateTime dataCriacao;

    @Column(length = 255)
    public String motivoFalha;

    public boolean processado = false;

    // ---- NOVOS ----
    @Column(length = 50)
    public String status = "PENDENTE";          // PENDENTE | REJEITADO

    public LocalDateTime dataRejeicao;

}