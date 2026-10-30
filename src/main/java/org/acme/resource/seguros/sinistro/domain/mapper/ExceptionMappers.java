package org.acme.resource.seguros.sinistro.infrastructure.config;

import jakarta.ws.rs.core.Response;
import org.acme.resource.seguros.sinistro.adapter.in.rest.dto.MensagemResponse;
import org.acme.resource.seguros.sinistro.exception.ContingenciaIndisponivelException;
import org.acme.resource.seguros.sinistro.exception.SinistroJaFechadoException;
import org.acme.resource.seguros.sinistro.exception.SinistroNaoEncontradoException;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

public class ExceptionMappers {

    @ServerExceptionMapper
    public RestResponse<MensagemResponse> naoEncontrado(SinistroNaoEncontradoException e) {
        return RestResponse.status(Response.Status.NOT_FOUND, new MensagemResponse(e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<MensagemResponse> conflito(SinistroJaFechadoException e) {
        return RestResponse.status(Response.Status.CONFLICT, new MensagemResponse(e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<MensagemResponse> contingenciaIndisponivel(ContingenciaIndisponivelException e) {
        return RestResponse.status(Response.Status.SERVICE_UNAVAILABLE, new MensagemResponse(e.getMessage()));
    }
}