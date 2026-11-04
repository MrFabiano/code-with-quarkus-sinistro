package org.acme.resource.seguros.sinistro.api.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MensagemResponse", description = "Mensagem simples de retorno ou erro")
public record MensagemResponse(@Schema(example = "Sinistro não encontrado com UUID: 123") String mensagem) {}