package org.acme.resource.seguros.sinistro.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.*;
import org.acme.resource.seguros.sinistro.domain.SinistroStatus;
import org.acme.resource.seguros.sinistro.infrastructure.config.MoedaBigDecimalDeserializer;
import org.acme.resource.seguros.sinistro.infrastructure.config.SinistroStatusDeserializer;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = false)
public record SinistroRequest(
        @NotBlank @Size(min = 4, message = "O ID da apólice deve ter no mínimo 4 caracteres")
        String apoliceId,

        @NotBlank @Size(min = 6)
        @Pattern(regexp = ".*[a-zA-Zà-úÀ-Ú].*", message = "A descrição deve conter pelo menos uma letra")
        String descricao,

        @NotNull @DecimalMin("0.01")
        @JsonDeserialize(using = MoedaBigDecimalDeserializer.class)
        BigDecimal valorEstimado,

        @NotNull(message = "O status é obrigatório")
        @JsonDeserialize(using = SinistroStatusDeserializer.class)
        SinistroStatus status


) {}