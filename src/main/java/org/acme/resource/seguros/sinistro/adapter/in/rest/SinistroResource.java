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

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/api/v1/sinistros")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Sinistros", description = "Abertura e gestão de sinistros de seguro de automóvel")
public class SinistroResource {

    @Inject SinistroService service;

    @POST
    @RunOnVirtualThread
    @Operation(summary = "Abre um sinistro",
            description = "Valida, persiste como ABERTO e publica o evento no Kafka. Se o Kafka falhar, tenta o Camel Router e, "
                    + "em último caso, grava o evento em fila de pendências (reprocessado automaticamente).")
    @RequestBody(required = true, content = @Content(
            schema = @Schema(implementation = SinistroRequest.class),
            examples = @ExampleObject(name = "Exemplo", value = """
                    {
                      "apoliceId": "AP-24248",
                      "descricao": "Colisão na Av. 23 de Maio, km 06",
                      "valorEstimado": "1.500,09",
                      "status": "ABERTO"
                    }""")))
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Sinistro criado e evento publicado",
                    content = @Content(schema = @Schema(implementation = SinistroResponse.class))),
            @APIResponse(responseCode = "202", description = "Sinistro criado; evento em contingência (Camel Router ou fila de pendências)",
                    content = @Content(schema = @Schema(implementation = MensagemResponse.class))),
            @APIResponse(responseCode = "400", description = "Falha de validação",
                    content = @Content(schema = @Schema(implementation = MensagemResponse.class))),
            @APIResponse(responseCode = "503", description = "Nem a contingência conseguiu registrar o evento",
                    content = @Content(schema = @Schema(implementation = MensagemResponse.class)))
    })
    public RestResponse<Object> criar(@Valid SinistroRequest request) {
        return switch (service.criar(request)) {
            case ResultadoCriacao.Criado c ->
                    RestResponse.status(Response.Status.CREATED, c.sinistro());
            case ResultadoCriacao.EmContingencia c ->
                    RestResponse.status(Response.Status.ACCEPTED, new MensagemResponse(c.mensagem()));
        };
    }

    @GET
    @RunOnVirtualThread
    @Operation(summary = "Lista os sinistros")
    @APIResponse(responseCode = "200", description = "Lista de sinistros",
            content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = SinistroResponse.class)))
    public List<SinistroResponse> listarTudo() {
        // O Panache traz todos os registros do Postgres em uma única linha
        return service.listarTodos();
    }

    @GET
    @Path("/{uuid}")
    @RunOnVirtualThread
    @Operation(summary = "Busca um sinistro pelo UUID")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Sinistro encontrado"),
            @APIResponse(responseCode = "404", description = "UUID inexistente",
                    content = @Content(schema = @Schema(implementation = MensagemResponse.class)))
    })
    public SinistroResponse buscar(
            @Parameter(description = "UUID do sinistro", example = "5d3abf04-ab4b-4592-a9e2-7e5e3f82347e")
            @PathParam("uuid") String uuid){
        return service.buscarPorUuid(uuid);   // lança SinistroNaoEncontradoException
    }

    @PUT
    @Path("/{uuid}")
    @RunOnVirtualThread
    @Operation(summary = "Atualiza um sinistro", description = "Atualiza os dados e o status. Não é possível fechar um sinistro já fechado.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Sinistro atualizado"),
            @APIResponse(responseCode = "400", description = "Falha de validação",
                    content = @Content(schema = @Schema(implementation = MensagemResponse.class))),
            @APIResponse(responseCode = "404", description = "UUID inexistente",
                    content = @Content(schema = @Schema(implementation = MensagemResponse.class))),
            @APIResponse(responseCode = "409", description = "Sinistro já está fechado",
                    content = @Content(schema = @Schema(implementation = MensagemResponse.class)))
    })
    public SinistroResponse atualizar(@PathParam("uuid") String uuid, @Valid SinistroUpdateRequest request) {
        return service.atualizar(uuid, request);
    }

    @DELETE
    @Path("/{uuid}")
    @RunOnVirtualThread
    @Operation(summary = "Remove um sinistro")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Removido"),
            @APIResponse(responseCode = "404", description = "UUID inexistente",
                    content = @Content(schema = @Schema(implementation = MensagemResponse.class)))
    })
    public RestResponse<Void> deletar(@PathParam("uuid") String uuid) {
        service.deletar(uuid);
        return RestResponse.noContent();
    }
}
