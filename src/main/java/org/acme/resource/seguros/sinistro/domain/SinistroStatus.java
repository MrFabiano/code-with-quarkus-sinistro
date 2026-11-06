package org.acme.resource.seguros.sinistro.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SinistroStatus {
    ABERTO, EM_ANALISE, FECHADO, CANCELADO;

    public boolean isFinal() {
        return this == FECHADO || this == CANCELADO;
    }

    public boolean podeIrPara(SinistroStatus destino) {
        return switch (this) {
            case ABERTO     -> destino == EM_ANALISE || destino == FECHADO || destino == CANCELADO;
            case EM_ANALISE -> destino == FECHADO   || destino == CANCELADO;
            case FECHADO, CANCELADO -> false;
        };
    }
}