package org.acme.resource.seguros.sinistro.producer;

import jakarta.enterprise.context.ApplicationScoped;
import org.acme.resource.seguros.sinistro.domain.model.SinistroEvent;

import java.math.BigDecimal;

@ApplicationScoped
public class SinistroProducerValidator {

    public boolean validarEvento(SinistroEvent evento) {
        return evento != null &&
                evento.apoliceId() != null && !evento.apoliceId().isBlank() &&
                evento.valorEstimado() != null &&
                evento.valorEstimado().compareTo(BigDecimal.ZERO) > 0 &&
                evento.uuid() != null;
    }
}