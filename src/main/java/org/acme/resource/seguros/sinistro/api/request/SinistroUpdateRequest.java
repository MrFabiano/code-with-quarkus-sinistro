package org.acme.resource.seguros.sinistro.api.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.acme.resource.seguros.sinistro.domain.SinistroStatus;
import org.acme.resource.seguros.sinistro.infrastructure.config.MoedaBigDecimalDeserializer;
import org.acme.resource.seguros.sinistro.infrastructure.config.SinistroStatusDeserializer;

import java.math.BigDecimal;

public record SinistroUpdateRequest(
        @NotBlank @Size(min = 4) String apoliceId,
        @NotBlank @Size(min = 6) String descricao,

        @NotNull @DecimalMin("0.01")
        @JsonDeserialize(using = MoedaBigDecimalDeserializer.class)
        BigDecimal valorEstimado,

        @NotNull(message = "O status é obrigatório")
        @JsonDeserialize(using = SinistroStatusDeserializer.class)
        SinistroStatus status
) {}