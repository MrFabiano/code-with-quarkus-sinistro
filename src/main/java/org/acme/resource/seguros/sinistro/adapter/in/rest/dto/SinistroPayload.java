package org.acme.resource.seguros.sinistro.adapter.in.rest.dto;

public record SinistroPayload(
        String uuid,
        String apoliceId,
        Double valorEstimado
) {}