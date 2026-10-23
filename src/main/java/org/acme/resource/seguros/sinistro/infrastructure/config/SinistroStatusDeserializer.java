package org.acme.resource.seguros.sinistro.infrastructure.config;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.acme.resource.seguros.sinistro.domain.SinistroStatus;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public class SinistroStatusDeserializer extends JsonDeserializer<SinistroStatus> {

    private static final String ACEITOS = Arrays.stream(SinistroStatus.values())
            .map(Enum::name)
            .collect(Collectors.joining(", "));

    @Override
    public SinistroStatus deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {

        // Trava principal: rejeita 0, 1, true, arrays e objetos
        if (!p.hasToken(JsonToken.VALUE_STRING)) {
            return (SinistroStatus) ctxt.handleUnexpectedToken(SinistroStatus.class, p);
        }

        String valor = p.getText().trim().toUpperCase(Locale.ROOT);

        try {
            return SinistroStatus.valueOf(valor);
        } catch (IllegalArgumentException e) {
            // weirdStringException sempre produz InvalidFormatException
            throw ctxt.weirdStringException(p.getText(), SinistroStatus.class,
                    "Valores aceitos: " + ACEITOS);
        }
    }
}