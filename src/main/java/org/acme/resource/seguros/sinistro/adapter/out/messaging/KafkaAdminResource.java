package org.acme.resource.seguros.sinistro.adapter.out.messaging;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Collections;
import java.util.Properties;

@Path("/api/v1/admin/kafka")
public class KafkaAdminResource {

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bootstrapServers;

    @DELETE
    @Path("/topic")
    @RolesAllowed("admin")
    public Response deletarTopico(@QueryParam("nome") String nomeTopico) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient adminClient = AdminClient.create(props)) {
            adminClient.deleteTopics(Collections.singletonList(nomeTopico)).all().get();
            return Response.ok("Tópico '" + nomeTopico + "' deletado com sucesso.").build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity("Erro ao deletar tópico: " + e.getMessage())
                    .build();
        }
    }
}