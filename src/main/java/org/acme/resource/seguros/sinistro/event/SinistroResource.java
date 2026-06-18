package org.acme.resource.seguros.sinistro.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.acme.resource.seguros.sinistro.model.DTO.MensagemResponse;
import org.acme.resource.seguros.sinistro.model.EventoPendente;
import org.acme.resource.seguros.sinistro.model.Sinistro;
import org.acme.resource.seguros.sinistro.producer.SinistroProducer;
import org.acme.resource.seguros.sinistro.service.SinistroService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/sinistros")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SinistroResource {

    @Inject
    SinistroService sinistroService;

    @POST
    @RunOnVirtualThread // Mantendo o uso de Virtual Threads do Java 21 para otimizar I/O
    public Response criarSinistro(Sinistro sinistro) {
        // O service abstrai o processamento e devolve o 201 ou 202 direto
        return sinistroService.executarProcessamento(sinistro);
    }

    @PUT
    @Path("/{uuid}")
    @Transactional
    @RunOnVirtualThread
    public Response atualizar(@PathParam("uuid") UUID uuid, Sinistro dadosAtualizados) {
        return Sinistro.<Sinistro>find("uuid", uuid)
                .singleResultOptional()
                .map(sinistroExistente -> {
                    sinistroExistente.apoliceId = dadosAtualizados.apoliceId;
                    sinistroExistente.descricao = dadosAtualizados.descricao;
                    sinistroExistente.valorEstimado = dadosAtualizados.valorEstimado;
                    sinistroExistente.status = dadosAtualizados.status;
                    return Response.ok(sinistroExistente).build();
                })
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(new MensagemResponse("Falha na atualizacao: Nao existe nenhum sinistro com o UUID '" + uuid + "'."))
                        .build());
    }

    @GET
    @RunOnVirtualThread // Mantendo a alta performance com Java 21
    public Response listarTudo() {
        // O Panache traz todos os registros do Postgres em uma única linha
        List<Sinistro> sinistros = Sinistro.listAll();

        if (sinistros.isEmpty()) {
            return Response.status(Response.Status.OK)
                    .entity(new MensagemResponse("Nenhum sinistro cadastrado ate o momento."))
                    .build();
        }

        return Response.ok(sinistros).build();
    }

    @GET
    @Path("/{uuid}")
    @RunOnVirtualThread
    public Response buscarPorUuid(@PathParam("uuid") UUID uuid) {
        return Sinistro.<Sinistro>find("uuid", uuid)
                .singleResultOptional()
                .map(sinistro -> Response.ok(sinistro).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(new MensagemResponse("O sinistro com o UUID '" + uuid + "' nao foi localizado."))
                        .build());
    }
    // O método que será chamado se o Kafka falhar
//    @Transactional
//    public Response fallbackCriarSinistro(Sinistro sinistro) throws JsonProcessingException {
//
//        System.err.println("!!! FALLBACK ACIONADO: Kafka indisponível. Salvando no banco de dados...");
//
//        // Cria o registro de segurança
//        EventoPendente pendente = new EventoPendente();
//        pendente.payloadJson = objectMapper.writeValueAsString(sinistro); // Ou converta para JSON real
//        pendente.dataCriacao = LocalDateTime.now();
//        pendente.motivoFalha = "Kafka Producer Unreachable";
//        pendente.persist();
//
//        return Response.status(Response.Status.ACCEPTED) // 202 Accepted indica que foi recebido, mas processamento posterior
//                .entity("Sinistro registrado com sucesso, mas a notificação está pendente devido a instabilidade no sistema de eventos.")
//                .build();
//    }

    @DELETE
    @Path("/{uuid}")
    @Transactional
    @RunOnVirtualThread
    public Response deletar(@PathParam("uuid") UUID uuid) {
        long linhasDeletadas = Sinistro.delete("uuid", uuid);

        if (linhasDeletadas > 0) {
            return Response.ok(new MensagemResponse("Sinistro com o UUID '" + uuid + "' removido com sucesso."))
                    .build();
        }

        return Response.status(Response.Status.NOT_FOUND)
                .entity(new MensagemResponse("Falha na exclusao: O sinistro com o UUID '" + uuid + "' nao foi encontrado."))
                .build();
    }

//    @Transactional(Transactional.TxType.REQUIRES_NEW) // <--- CRÍTICO: Abre uma nova transação isolada
//    public Response fallbackCriarSinistro(Sinistro sinistro) {
//        System.err.println("!!! FALLBACK ACIONADO: Kafka indisponivel. Salvando contingencia no banco...");
//
//        try {
//            // 1. Persiste o Sinistro original primeiro (já que a transação principal sofreu rollback)
//            sinistro.id = null; // Garante que é um insert limpo
//            sinistro.persist();
//
//            // 2. Cria o registro de segurança na tabela de eventos pendentes
//            EventoPendente pendente = new EventoPendente();
//            pendente.payloadJson = objectMapper.writeValueAsString(sinistro);
//            pendente.dataCriacao = LocalDateTime.now();
//            pendente.motivoFalha = "Kafka Producer Unreachable";
//            pendente.persist();
//
//            // 3. Retorna o JSON padronizado com HTTP 202
//            return Response.status(Response.Status.ACCEPTED)
//                    .entity(new MensagemResponse("Sinistro registrado com sucesso, mas a notificacao esta pendente devido a instabilidade no sistema de eventos."))
//                    .build();
//
//        } catch (JsonProcessingException e) {
//            System.err.println("Erro grave de serializacao no fallback: " + e.getMessage());
//            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
//                    .entity(new MensagemResponse("Erro interno ao processar a contingencia do sinistro."))
//                    .build();
//        }
//    }
}