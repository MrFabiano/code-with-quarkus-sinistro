package org.acme.resource.seguros.sinistro.infrastructure.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.EventoPendente;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@ApplicationScoped
@Readiness
public class ContingenciaHealthCheck implements HealthCheck {

    private static final int THRESHOLD_PENDENCIAS = 100;

    @Override
    public HealthCheckResponse call() {
        long pendentes = EventoPendente.count();

        if (pendentes > THRESHOLD_PENDENCIAS) {
            return HealthCheckResponse
                    .named("Eventos Pendentes (Contingência)")
                    .down()
                    .withData("count", String.valueOf(pendentes))
                    .withData("threshold", String.valueOf(THRESHOLD_PENDENCIAS))
                    .withData("status", "HIGH")
                    .build();
        }

        return HealthCheckResponse
                .named("Eventos Pendentes (Contingência)")
                .up()
                .withData("count", String.valueOf(pendentes))
                .withData("threshold", String.valueOf(THRESHOLD_PENDENCIAS))
                .withData("status", "NORMAL")
                .build();
    }
}