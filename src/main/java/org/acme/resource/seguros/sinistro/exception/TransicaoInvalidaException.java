package org.acme.resource.seguros.sinistro.exception;

import org.acme.resource.seguros.sinistro.domain.SinistroStatus;

public class TransicaoInvalidaException extends RuntimeException {

    public TransicaoInvalidaException(SinistroStatus atual, SinistroStatus destino) {
        super("Transição de status não permitida: " + atual + " -> " + destino);
    }
}