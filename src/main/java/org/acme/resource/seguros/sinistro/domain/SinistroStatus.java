package org.acme.resource.seguros.sinistro.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SinistroStatus {

    ABERTO,
    FECHADO;

    @JsonValue
    public String toValue() {
        return name();
    }
}