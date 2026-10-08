package org.acme.resource.seguros.sinistro.infrastructure.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.resource.seguros.sinistro.domain.port.output.FriendlyResponse;

@Provider
public class JsonExceptionMapper implements ExceptionMapper<JsonProcessingException> {

    @Override
    public Response toResponse(JsonProcessingException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new FriendlyResponse("JSON Inválido", "O corpo da requisição contém um JSON malformado ou campos com tipos incorretos."))
                .build();
    }
}