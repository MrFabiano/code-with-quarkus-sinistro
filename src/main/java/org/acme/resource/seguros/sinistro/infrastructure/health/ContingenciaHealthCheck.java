package org.acme.resource.seguros.sinistro.infrastructure.health;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.EventoPendente;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@ApplicationScoped
@Readiness
public class ContingenciaHealthCheck implements HealthCheck {

    private static final long THRESHOLD = 100;

    @Override
    public HealthCheckResponse call() {
        long pendentes = QuarkusTransaction.requiringNew()
                .call(() -> EventoPendente.count("status = ?1 and processado = false", "PENDENTE"));

        return HealthCheckResponse.named("Eventos Pendentes")
                .status(pendentes <= THRESHOLD)
                .withData("count", pendentes)
                .withData("threshold", THRESHOLD)
                .build();
    }
}