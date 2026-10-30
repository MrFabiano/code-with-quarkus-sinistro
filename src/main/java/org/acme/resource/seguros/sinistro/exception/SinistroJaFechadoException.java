package org.acme.resource.seguros.sinistro.exception;

public class SinistroJaFechadoException extends RuntimeException {
    public SinistroJaFechadoException() { super("Sinistro já está fechado."); }
}