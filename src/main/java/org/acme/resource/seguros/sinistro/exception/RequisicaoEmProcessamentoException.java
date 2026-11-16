package org.acme.resource.seguros.sinistro.exception;

public class RequisicaoEmProcessamentoException extends RuntimeException {
    public RequisicaoEmProcessamentoException() {
        super("Requisição com esta chave ainda está em processamento. Tente novamente em instantes.");
    }
}