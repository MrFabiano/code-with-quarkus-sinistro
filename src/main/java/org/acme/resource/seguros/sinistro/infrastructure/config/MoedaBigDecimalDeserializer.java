package org.acme.resource.seguros.sinistro.infrastructure.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.math.BigDecimal;

public class MoedaBigDecimalDeserializer extends JsonDeserializer<BigDecimal> {
    @Override
    public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {

        JsonToken token = p.currentToken();

        // CASO 1: Número JSON puro (ex: 1590.00)
        if (token.isNumeric()) {
            // Usa o parser nativo, já interpretou ponto decimal corretamente
            return p.getDecimalValue();
        }

        // CASO 2: String formatada (ex: "1.590,00", "R$ 1.590,00")
        if (token == JsonToken.VALUE_STRING) {
            String value = p.getText();

            if (value == null || value.isBlank()) {
                return null;
            }

            // Remove prefixos de moeda e espaços
            value = value.replaceAll("[R$€$\\s]", "").trim();

            // Lógica inteligente:
            // - Se tiver ponto E vírgula: último separador é o decimal
            // - Ex: "1.590,00" → último separador = "," → decimal brasileiro
            // - Ex: "1,590.00" → último separador = "." → decimal americano

            int lastDot = value.lastIndexOf('.');
            int lastComma = value.lastIndexOf(',');

            if (lastDot > lastComma && lastComma >= 0) {
                // Formato americano: 1,590.00 (último é ponto = decimal)
                value = value.replace(",", ""); // Remove milhar
            } else if (lastComma > lastDot) {
                // Formato brasileiro: 1.590,00 (último é vírgula = decimal)
                value = value.replace(".", "")    // Remove milhar
                        .replace(",", ".");     // Converte decimal
            } else if (lastComma >= 0 && lastDot == -1) {
                // Só vírgula: pode ser decimal brasileiro sem milhar (150,00)
                value = value.replace(",", ".");
            }
            // Só ponto: assume decimal americano (já é válido)

            try {
                return new BigDecimal(value);
            } catch (NumberFormatException e) {
                throw ctxt.weirdStringException(value, BigDecimal.class,
                        "Formato de moeda inválido. Use: 1.590,00 ou R$ 1.590,00");
            }
        }

        // Qualquer outro token (null, array, objeto)
        return (BigDecimal) ctxt.handleUnexpectedToken(BigDecimal.class, p);
    }
}
