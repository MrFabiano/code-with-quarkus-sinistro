package org.acme.resource.seguros.sinistro.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SinistroEvent(
        String uuid,
        String tipoEvento,      // <-- 2º argumento
        String apoliceId,
        String descricao,
        Double valorEstimado,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dataHora  // <-- 7º argumento
) {}
