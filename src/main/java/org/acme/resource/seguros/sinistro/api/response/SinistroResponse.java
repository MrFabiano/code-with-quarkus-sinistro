package org.acme.resource.seguros.sinistro.api.response;

import org.acme.resource.seguros.sinistro.adapter.out.persistence.Sinistro;
import org.acme.resource.seguros.sinistro.domain.SinistroStatus;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(name = "SinistroResponse", description = "Sinistro registrado")
public record SinistroResponse(
        @Schema(example = "5d3abf04-ab4b-4592-a9e2-7e5e3f82347e") String uuid,
        @Schema(example = "AP-24248") String apoliceId,
        @Schema(example = "Colisão na Av. 23 de Maio, km 06") String descricao,
        @Schema(type = SchemaType.NUMBER, example = "1500.09") BigDecimal valorEstimado,
        SinistroStatus status
) {}