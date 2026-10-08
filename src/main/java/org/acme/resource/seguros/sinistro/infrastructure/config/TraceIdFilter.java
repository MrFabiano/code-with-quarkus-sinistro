package org.acme.resource.seguros.sinistro.infrastructure.config;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;
@Provider
@Priority(Priorities.USER)
public class TraceIdFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String traceId = extractTraceId(requestContext);

        // Adiciona ao MDC para logs
        MDC.put("trace-id", traceId);

        // Adiciona ao header da resposta
        requestContext.setProperty("trace-id", traceId);
    }

    private String extractTraceId(ContainerRequestContext requestContext) {
        // Tenta pegar do header X-B3-TraceId (Zipkin/B3)
        String traceHeader = requestContext.getHeaderString("X-B3-TraceId");
        if (traceHeader != null && !traceHeader.isBlank()) {
            return traceHeader;
        }

        // Tenta pegar do header X-Request-ID
        traceHeader = requestContext.getHeaderString("X-Request-ID");
        if (traceHeader != null && !traceHeader.isBlank()) {
            return traceHeader;
        }

        // Gera um novo se não existir
        return UUID.randomUUID().toString().substring(0, 16);
    }
}
