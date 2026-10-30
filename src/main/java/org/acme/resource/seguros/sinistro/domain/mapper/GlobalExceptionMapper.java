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
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Provider
@Priority(Priorities.USER)
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        // 1. Desenvelopa exceções encapsuladas por Threads e Mutiny/CompletableFuture
        Throwable realCause = unwrapException(exception);

        LOG.error("|| GLOBAL ERROR HANDLER || Exceção processada: " + realCause.getClass().getName(), realCause);

        // 2. Se a exceção já for do JAX-RS / WebApplicationException, reaproveita a resposta original
        if (realCause instanceof WebApplicationException wae) {
            Response originalResponse = wae.getResponse();

            // Se a resposta original já tiver uma entidade montada, retorna ela diretamente
            if (originalResponse.hasEntity()) {
                return originalResponse;
            }

            int statusCode = originalResponse.getStatus();
            return Response.status(statusCode)
                    .entity(new FriendlyResponse(
                            "Erro " + statusCode,
                            realCause.getMessage() != null ? realCause.getMessage() : "Falha na requisição HTTP"
                    ))
                    .build();
        }

        // 3. Fallback para exceções não tratadas (Erros reais de runtime)
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new FriendlyResponse(
                        "Erro 500",
                        "Erro Interno: " + getRootMessage(realCause)
                ))
                .build();
    }

    private Throwable unwrapException(Throwable t) {
        Throwable cause = t;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    private String getRootMessage(Throwable t) {
        if (t.getMessage() != null && !t.getMessage().isBlank()) {
            return t.getMessage();
        }
        return t.getClass().getSimpleName();
    }
}