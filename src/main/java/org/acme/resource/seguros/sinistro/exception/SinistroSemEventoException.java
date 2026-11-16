package org.acme.resource.seguros.sinistro.exception;

public class SinistroSemEventoException extends ContingenciaIndisponivelException {

    private final String uuid;

    public SinistroSemEventoException(String uuid, Throwable causa) {
        super("Sinistro " + uuid + " foi registrado, mas o evento não pôde ser registrado. "
                + "Não repita com outra chave; contate o suporte.", causa);
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }
}