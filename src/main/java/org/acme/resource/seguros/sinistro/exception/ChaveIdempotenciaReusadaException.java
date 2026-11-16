package org.acme.resource.seguros.sinistro.exception;

public class ChaveIdempotenciaReusadaException extends RuntimeException {
    public ChaveIdempotenciaReusadaException() {
        super("A chave de idempotência já foi usada com um conteúdo diferente.");
    }
}