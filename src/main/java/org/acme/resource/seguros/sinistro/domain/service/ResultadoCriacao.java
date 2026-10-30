package org.acme.resource.seguros.sinistro.domain.service;

import org.acme.resource.seguros.sinistro.api.response.SinistroResponse;

public sealed interface ResultadoCriacao {
    record Criado(SinistroResponse sinistro) implements ResultadoCriacao {}
    record EmContingencia(String mensagem) implements ResultadoCriacao {}
}