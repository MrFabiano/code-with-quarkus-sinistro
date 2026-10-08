package org.acme.resource.seguros.sinistro.domain.port.output;

public class FriendlyResponse {

    public String status;
    public String mensagem;

    public FriendlyResponse(String status, String mensagem) {
        this.status = status;
        this.mensagem = mensagem;
    }
}
