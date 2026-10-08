package org.acme.resource.seguros.sinistro.infrastructure.health;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import java.sql.Connection;
import java.sql.SQLException;

@ApplicationScoped
@Liveness
public class DatabaseHealthCheck implements HealthCheck {

    @Inject
    AgroalDataSource dataSource;

    @Override
    public HealthCheckResponse call() {
        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(5);

            if (!isValid) {
                return HealthCheckResponse
                        .named("Database Connection")
                        .down()
                        .withData("error", "Conexão inválida")
                        .build();
            }

            return HealthCheckResponse
                    .named("Database Connection")
                    .up()
                    .withData("connected", "true")
                    .build();

        } catch (SQLException e) {
            return HealthCheckResponse
                    .named("Database Connection")
                    .down()
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}