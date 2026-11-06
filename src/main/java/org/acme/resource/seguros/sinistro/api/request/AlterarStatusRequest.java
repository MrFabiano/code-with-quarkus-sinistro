package org.acme.resource.seguros.sinistro.api.request;

import io.smallrye.common.constraint.NotNull;
import org.acme.resource.seguros.sinistro.domain.SinistroStatus;

public record AlterarStatusRequest(@NotNull SinistroStatus status) {}
