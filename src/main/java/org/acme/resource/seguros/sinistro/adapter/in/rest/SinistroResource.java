package org.acme.resource.seguros.sinistro.adapter.in.rest;

import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.resource.seguros.sinistro.adapter.in.rest.dto.MensagemResponse;
import org.acme.resource.seguros.sinistro.api.request.SinistroRequest;
import org.acme.resource.seguros.sinistro.api.request.SinistroUpdateRequest;
import org.acme.resource.seguros.sinistro.api.response.*;
import org.acme.resource.seguros.sinistro.domain.service.ResultadoCriacao;
import org.acme.resource.seguros.sinistro.domain.service.SinistroService;
import jakarta.ws.rs.*;

import java.util.List;

import org.jboss.resteasy.reactive.RestResponse;

@Path("/api/v1/sinistros")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SinistroResource {

    @Inject SinistroService service;

    @POST
    @RunOnVirtualThread
    public RestResponse<Object> criar(@Valid SinistroRequest request) {
        return switch (service.criar(request)) {
            case ResultadoCriacao.Criado c ->
                    RestResponse.status(Response.Status.CREATED, c.sinistro());
            case ResultadoCriacao.EmContingencia c ->
                    RestResponse.status(Response.Status.ACCEPTED, new MensagemResponse(c.mensagem()));
        };
    }

    @GET
    @RunOnVirtualThread // Mantendo a alta performance com Java 21
    public List<SinistroResponse> listarTudo() {
        // O Panache traz todos os registros do Postgres em uma única linha
        return service.listarTodos();
    }

    @GET
    @Path("/{uuid}")
    @RunOnVirtualThread
    public SinistroResponse buscar(@PathParam("uuid") String uuid) {
        return service.buscarPorUuid(uuid);   // lança SinistroNaoEncontradoException
    }

    @PUT
    @Path("/{uuid}")
    @RunOnVirtualThread
    public SinistroResponse atualizar(@PathParam("uuid") String uuid,
                                      @Valid SinistroUpdateRequest request) {
        return service.atualizar(uuid, request);
    }

    @DELETE
    @Path("/{uuid}")
    @RunOnVirtualThread
    public RestResponse<Void> deletar(@PathParam("uuid") String uuid) {
        service.deletar(uuid);
        return RestResponse.noContent();
    }
}
