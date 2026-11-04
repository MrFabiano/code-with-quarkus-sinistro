package org.acme.resource.seguros.sinistro.api.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.acme.resource.seguros.sinistro.domain.SinistroStatus;
import org.acme.resource.seguros.sinistro.infrastructure.config.MoedaBigDecimalDeserializer;
import org.acme.resource.seguros.sinistro.infrastructure.config.SinistroStatusDeserializer;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "SinistroUpdateRequest", description = "Dados para abertura de um sinistro")
public record SinistroUpdateRequest(

        @Schema(description = "Identificador da apólice (mínimo 4 caracteres)", example = "AP-24248", required = true)
        @NotBlank @Size(min = 4) String apoliceId,

        @Schema(description = "Descrição do ocorrido (mínimo 6 caracteres, com ao menos uma letra)",
                example = "Colisão na Av. 23 de Maio, km 06", required = true)
        @NotBlank @Size(min = 6) String descricao,

        @Schema(description = "Valor estimado. Aceita número (1500.09) ou texto brasileiro (\"1.500,09\", \"R$ 1.500,09\")",
                type = SchemaType.STRING, example = "1.500,09", required = true)
        @JsonDeserialize(using = MoedaBigDecimalDeserializer.class)
        @NotNull @DecimalMin("0.01") BigDecimal valorEstimado,

        @Schema(description = "Obrigatório e validado, mas o servidor sempre grava o sinistro como ABERTO",
                example = "ABERTO", required = true)
        @JsonDeserialize(using = SinistroStatusDeserializer.class)
        @NotNull(message = "O status é obrigatório") SinistroStatus status
) {}