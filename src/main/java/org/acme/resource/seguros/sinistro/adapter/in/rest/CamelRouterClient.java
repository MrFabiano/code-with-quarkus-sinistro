package org.acme.resource.seguros.sinistro.adapter.in.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.acme.resource.seguros.sinistro.adapter.in.rest.dto.ContingenciaResponseDTO;
import org.acme.resource.seguros.sinistro.adapter.in.rest.dto.SinistroPayload;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.Sinistro;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "camel-router", baseUri = "http://localhost:8081")
public interface CamelRouterClient {

    @POST
    @Path("/api/v1/contingencia")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    ContingenciaResponseDTO enviarContingencia(SinistroPayload payload);
}