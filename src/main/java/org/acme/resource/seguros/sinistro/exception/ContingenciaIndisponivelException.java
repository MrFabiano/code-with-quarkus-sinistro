package org.acme.resource.seguros.sinistro.exception;

public class ContingenciaIndisponivelException extends RuntimeException {
    public ContingenciaIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
