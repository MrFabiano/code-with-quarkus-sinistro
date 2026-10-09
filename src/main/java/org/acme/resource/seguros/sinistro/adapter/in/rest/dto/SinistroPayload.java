package org.acme.resource.seguros.sinistro.adapter.in.rest.dto;

import java.math.BigDecimal;

public record SinistroPayload(
        String uuid,
        String apoliceId,
        BigDecimal valorEstimado
) {}