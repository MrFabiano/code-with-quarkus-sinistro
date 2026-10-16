package org.acme.resource.seguros.sinistro.infrastructure.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.math.BigDecimal;

public class MoedaBigDecimalDeserializer extends JsonDeserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {

        String value = p.getText().trim();

        // Se for null ou vazio, retorna null
        if (value == null || value.isBlank()) {
            return null;
        }

        // Remove R$, R$, €, $, espaços, pontos de milhar
        value = value.replaceAll("[R$€$\\s]", "")  // Remove símbolos de moeda
                .replaceAll("\\.", "");      // Remove ponto (milhar)

        // Troca vírgula por ponto para parsing
        value = value.replace(",", ".");

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Formato de moeda inválido: " + value);
        }
    }
}