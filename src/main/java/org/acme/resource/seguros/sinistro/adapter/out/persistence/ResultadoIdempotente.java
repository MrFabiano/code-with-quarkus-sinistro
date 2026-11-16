package org.acme.resource.seguros.sinistro.adapter.out.persistence;

import org.acme.resource.seguros.sinistro.domain.service.ResultadoCriacao;

public record ResultadoIdempotente(ResultadoCriacao resultado, boolean repetido) {}