package org.acme.resource.seguros.sinistro.domain.mapper;

import jakarta.persistence.OptimisticLockException;
import jakarta.ws.rs.core.Response;
import org.acme.resource.seguros.sinistro.adapter.in.rest.dto.MensagemResponse;
import org.acme.resource.seguros.sinistro.exception.*;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.util.Map;

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

    @ServerExceptionMapper
    public RestResponse<Map<String, String>> transicaoInvalida(TransicaoInvalidaException e) {
        return RestResponse.status(Response.Status.CONFLICT, Map.of("mensagem", e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<Map<String, String>> conflitoVersao(OptimisticLockException e) {
        return RestResponse.status(Response.Status.CONFLICT,
                Map.of("mensagem", "O sinistro foi alterado por outra operação. Consulte e tente novamente."));
    }
    @ServerExceptionMapper
    public RestResponse<Map<String, String>> erroGenerico(Exception e) {
        //LOG.error("Erro não tratado", e);                       // stack trace completo no log
        Throwable causa = e.getCause() != null ? e.getCause() : e;
        if (causa instanceof OptimisticLockException) {
            return RestResponse.status(Response.Status.CONFLICT,
                    Map.of("mensagem", "O sinistro foi alterado por outra operação. Tente novamente."));
        }
        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR,
                Map.of("mensagem", "Erro interno"));
    }

    @ServerExceptionMapper
    public RestResponse<Object> mapear(ChaveIdempotenciaReusadaException e) {
        return RestResponse.ResponseBuilder
                .create(422, String.valueOf(new MensagemResponse(e.getMessage())))
                .build();
    }
    @ServerExceptionMapper
    public RestResponse<MensagemResponse> mapear(RequisicaoEmProcessamentoException e) {
        return RestResponse.ResponseBuilder
                .create(Response.Status.CONFLICT, new MensagemResponse(e.getMessage()))
                .header("Retry-After", "1")
                .build();
    }

}