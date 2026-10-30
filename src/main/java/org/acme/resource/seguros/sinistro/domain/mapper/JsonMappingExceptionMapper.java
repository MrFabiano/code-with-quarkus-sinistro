package org.acme.resource.seguros.sinistro.domain.mapper;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.resource.seguros.sinistro.domain.port.output.FriendlyResponse;

@Provider
public class JsonMappingExceptionMapper implements ExceptionMapper<JsonMappingException> {

    @Override
    public Response toResponse(JsonMappingException exception) {

        String campo = exception.getPath().isEmpty()
                ? null
                : exception.getPath().get(exception.getPath().size() - 1).getFieldName();

        String detalhe = exception instanceof InvalidFormatException ife
                ? ife.getOriginalMessage()
                : "O corpo da requisição contém um valor com tipo ou formato inválido.";

        String mensagem = (campo == null || campo.isBlank())
                ? detalhe
                : campo + ": " + detalhe;

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new FriendlyResponse("Erro de Validação", mensagem))
                .build();
    }
}