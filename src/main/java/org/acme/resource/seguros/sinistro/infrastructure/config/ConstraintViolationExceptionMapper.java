package org.acme.resource.seguros.sinistro.infrastructure.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.resource.seguros.sinistro.domain.port.output.FriendlyResponse;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        Set<ConstraintViolation<?>> violations = exception.getConstraintViolations();
        List<String> messages = violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.toList());

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new FriendlyResponse("Erro de Validação",
                        String.join("; ", messages)))
                .build();
    }
}