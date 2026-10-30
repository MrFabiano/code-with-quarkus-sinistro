package org.acme.resource.seguros.sinistro.api.response;

import org.acme.resource.seguros.sinistro.adapter.out.persistence.Sinistro;
import org.acme.resource.seguros.sinistro.domain.SinistroStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SinistroResponse(
        String uuid,
        String apoliceId,
        String descricao,
        BigDecimal valorEstimado,
        SinistroStatus status
) {}