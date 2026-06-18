package org.acme.resource.seguros.sinistro.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record SinistroEvent(
        UUID uuid,
        String tipoEvento,      // <-- 2º argumento
        String apoliceId,
        String descricao,
        Double valorEstimado,
        String status,
        LocalDateTime dataHora  // <-- 7º argumento
) {}
