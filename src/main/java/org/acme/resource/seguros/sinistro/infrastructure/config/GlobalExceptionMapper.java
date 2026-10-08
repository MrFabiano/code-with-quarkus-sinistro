package org.acme.resource.seguros.sinistro.infrastructure.config;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.resource.seguros.sinistro.domain.port.output.FriendlyResponse;
import org.jboss.logging.Logger;

@Provider
@Priority(Priorities.USER)
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        LOG.error("|| GLOBAL ERROR HANDLER || Unexpected exception", exception);

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new FriendlyResponse(
                        "Erro Interno",
                        "Ocorreu um erro inesperado. Consulte os logs para mais detalhes."
                ))
                .build();
    }
}
