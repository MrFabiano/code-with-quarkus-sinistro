package org.acme.resource.seguros.sinistro.exception;

public class SinistroNaoEncontradoException extends RuntimeException {
    public SinistroNaoEncontradoException(String uuid) {
        super("Sinistro não encontrado com UUID: " + uuid);
    }
}