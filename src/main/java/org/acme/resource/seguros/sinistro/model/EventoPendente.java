package org.acme.resource.seguros.sinistro.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;

@Entity
public class EventoPendente extends PanacheEntity{

    public String payloadJson; // O JSON do seu Sinistro
    public LocalDateTime dataCriacao;
    public String motivoFalha;

}