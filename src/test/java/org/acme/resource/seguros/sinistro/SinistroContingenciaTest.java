package org.acme.resource.seguros.sinistro;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.UUID;

import org.acme.resource.seguros.sinistro.adapter.in.rest.CamelRouterClient;
import org.acme.resource.seguros.sinistro.adapter.in.rest.dto.ContingenciaResponseDTO;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.EventoPendente;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.Sinistro;
import org.acme.resource.seguros.sinistro.domain.service.ContingenciaLocalService;
import org.acme.resource.seguros.sinistro.domain.service.SinistroScheduler;
import org.acme.resource.seguros.sinistro.producer.SinistroProducer;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Cenários de contingência do sinistro-api.
 *
 * Kafka e Camel Router são simulados com mocks (mais rápido e determinístico que derrubar
 * containers). O banco é o Postgres real (Dev Services ou o local).
 *
 * Ajuste os imports de pacote (SinistroProducer, SinistroScheduler, EventoPendente, etc.)
 * para o pacote real do seu projeto.
 */
@QuarkusTest
class SinistroContingenciaTest {

    private static final String BODY = """
            {
              "apoliceId": "AP-24248",
              "descricao": "Acidente na 23 de maio, km 06 proximo ao centro",
              "valorEstimado": "1.500,09",
              "status": "ABERTO"
            }
            """;

    @InjectMock
    SinistroProducer producer;

    @InjectMock
    @RestClient
    CamelRouterClient camel;

    // Spy: usa a implementação real, e o cenário 7 força a falha
    @InjectSpy
    ContingenciaLocalService contingenciaLocal;

    @Inject
    SinistroScheduler scheduler;

    @AfterEach
    void resetarSpy() {
        Mockito.reset(contingenciaLocal);
    }

    @BeforeEach
    void limpar() {
        QuarkusTransaction.requiringNew().run(() -> {
            EventoPendente.deleteAll();
            Sinistro.deleteAll();
        });
    }

    // ---------------------------------------------------------------
    // 1. Kafka + Router ativos -> 201
    // ---------------------------------------------------------------
    @Test
    void nivel1_kafkaAtivo_retorna201() {
        doNothing().when(producer).publicar(any());

        given().contentType("application/json").body(BODY)
                .when().post("/api/v1/sinistros")
                .then().statusCode(201)
                .body("uuid", notNullValue())
                .body("status", equalTo("ABERTO"))
                .body("valorEstimado", equalTo(1500.09f));

        verify(camel, never()).enviarContingencia(any());
        assertEquals(0, contarPendentes());
    }

    // ---------------------------------------------------------------
    // 2. Kafka falha no Quarkus, Router publica -> 202 via Camel
    // ---------------------------------------------------------------
    @Test
    void nivel2_kafkaFalha_routerProcessa_retorna202() {
        doThrow(new RuntimeException("kafka fora")).when(producer).publicar(any());
        when(camel.enviarContingencia(any()))
                .thenReturn(new ContingenciaResponseDTO("processed", UUID.randomUUID().toString()));

        given().contentType("application/json").body(BODY)
                .when().post("/api/v1/sinistros")
                .then().statusCode(202)
                .body("mensagem", equalTo("Processado via Camel Router"));

        assertEquals(0, contarPendentes());
    }

    // ---------------------------------------------------------------
    // 3. Kafka + Router fora -> 202 e linha PENDENTE em eventopendente
    // ---------------------------------------------------------------
    @Test
    void nivel3_kafkaERouterFora_gravaEventoPendente() {
        doThrow(new RuntimeException("kafka fora")).when(producer).publicar(any());
        when(camel.enviarContingencia(any())).thenThrow(new RuntimeException("router fora"));

        given().contentType("application/json").body(BODY)
                .when().post("/api/v1/sinistros")
                .then().statusCode(202);

        assertEquals(1, contarPendentes());
    }

    // ---------------------------------------------------------------
    // 4. Kafka religado -> scheduler reenvia e remove
    // ---------------------------------------------------------------
    @Test
    void nivel4_kafkaReligado_schedulerReenviaERemove() {
        doThrow(new RuntimeException("kafka fora")).when(producer).publicar(any());
        when(camel.enviarContingencia(any())).thenThrow(new RuntimeException("router fora"));

        given().contentType("application/json").body(BODY)
                .when().post("/api/v1/sinistros")
                .then().statusCode(202);
        assertEquals(1, contarPendentes());

        // Kafka volta
        doNothing().when(producer).publicar(any());
        scheduler.reprocessarEventosPendentes();

        assertEquals(0, contarPendentes());
        // 1 tentativa na criação (que falhou) + 1 no reenvio
        verify(producer, times(2)).publicar(any());
    }

    // ---------------------------------------------------------------
    // 5. Payload inválido -> REJEITADO (+ tentativa de envio à DLQ)
    //    Obs.: sem broker no teste, o envio à DLQ falha e é apenas logado
    //    (o código já trata isso). Pode levar até max.block.ms (10 s).
    //    Para validar a DLQ de fato, use um teste de integração com Kafka real.
    // ---------------------------------------------------------------
    @Test
    void nivel5_payloadInvalido_viraRejeitado() {
        Long id = QuarkusTransaction.requiringNew().call(() -> {
            EventoPendente e = new EventoPendente();
            e.uuid = UUID.randomUUID().toString();
            e.payloadJson = "{\"uuid\":\"" + e.uuid + "\",\"descricao\":\"sem apolice\",\"valorEstimado\":10}";
            e.dataCriacao = LocalDateTime.now();
            e.motivoFalha = "teste";
            e.status = "PENDENTE";
            e.persist();
            return e.id;
        });

        scheduler.reprocessarEventosPendentes();

        String status = QuarkusTransaction.requiringNew()
                .call(() -> ((EventoPendente) EventoPendente.findById(id)).status);
        assertEquals("REJEITADO", status);
        verify(producer, never()).publicar(any());   // inválido nunca chega ao Kafka
    }

    // ---------------------------------------------------------------
    // 6. Fronteira: 202 e, com o Kafka de volta, 201 sem reiniciar a API
    // ---------------------------------------------------------------
    @Test
    void nivel6_transicao202Para201_semReinicio() {
        doThrow(new RuntimeException("kafka fora")).when(producer).publicar(any());
        when(camel.enviarContingencia(any())).thenThrow(new RuntimeException("router fora"));

        given().contentType("application/json").body(BODY)
                .when().post("/api/v1/sinistros")
                .then().statusCode(202);

        doNothing().when(producer).publicar(any());

        given().contentType("application/json").body(BODY)
                .when().post("/api/v1/sinistros")
                .then().statusCode(201);
    }

    // ---------------------------------------------------------------
    // 7. Kafka + Router + banco de contingência fora -> 503 (nunca 202)
    // ---------------------------------------------------------------
    @Test
    void nivel7_tudoFora_retorna503() {
        doThrow(new RuntimeException("kafka fora")).when(producer).publicar(any());
        when(camel.enviarContingencia(any())).thenThrow(new RuntimeException("router fora"));
        doThrow(new RuntimeException("postgres fora"))
                .when(contingenciaLocal).gravarContingencia(any(), any());

        given().contentType("application/json").body(BODY)
                .when().post("/api/v1/sinistros")
                .then().statusCode(503);
    }

    // ---------------------------------------------------------------
    private long contarPendentes() {
        return QuarkusTransaction.requiringNew()
                .call(() -> EventoPendente.count("status = ?1", "PENDENTE"));
    }
}

