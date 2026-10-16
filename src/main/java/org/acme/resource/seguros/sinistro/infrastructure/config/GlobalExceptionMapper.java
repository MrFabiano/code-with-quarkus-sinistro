package org.acme.resource.seguros.sinistro.infrastructure.config;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.resource.seguros.sinistro.domain.port.output.FriendlyResponse;
import org.jboss.logging.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Provider
@Priority(Priorities.USER)
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        LOG.error("|| GLOBAL ERROR HANDLER || Unexpected exception", exception);

        boolean isDevMode = "dev".equals(System.getProperty("quarkus.profile"))
                || "dev".equals(System.getenv("QUARKUS_PROFILE"));

        Response.Status status;
        if (exception instanceof WebApplicationException wae) {
            status = Response.Status.fromStatusCode(wae.getResponse().getStatus());
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR;
        }

        return Response.status(status)
                .entity(new FriendlyResponse(
                        "Erro " + status.getStatusCode(),
                        isDevMode ? exception.getMessage() : "Ocorreu um erro inesperado."
                ))
                .build();
    }
}