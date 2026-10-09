package org.acme.resource.seguros.sinistro.adapter.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;

@Entity
public class EventoPendente extends PanacheEntity{

    public String payloadJson;
    public LocalDateTime dataCriacao;
    public String motivoFalha;
    public boolean processado = false;

}