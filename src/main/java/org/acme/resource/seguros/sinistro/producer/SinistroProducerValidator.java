package org.acme.resource.seguros.sinistro.producer;

import jakarta.enterprise.context.ApplicationScoped;
import org.acme.resource.seguros.sinistro.domain.model.SinistroEvent;

@ApplicationScoped
public class SinistroProducerValidator {

    public boolean validarEvento(SinistroEvent evento) {
        return evento != null &&
                evento.apoliceId() != null && !evento.apoliceId().isBlank() &&
                evento.valorEstimado() != null && evento.valorEstimado() > 0 &&
                evento.uuid() != null;
    }
}