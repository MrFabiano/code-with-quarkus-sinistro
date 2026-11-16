package org.acme.resource.seguros.sinistro;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
class SinistroIdempotenciaTest {

    private static String corpo(String descricao) {
        return """
                {"apoliceId":"AP-IDEM-1","descricao":"%s","valorEstimado":"1.500,09"}
                """.formatted(descricao);
    }

    private static Response post(String chave, String corpo) {
        var req = given().contentType(ContentType.JSON).body(corpo);
        if (chave != null) {
            req = req.header("Idempotency-Key", chave);
        }
        return req.post("/api/v1/sinistros");
    }

    private static int contar(String descricao) {
        return given().get("/api/v1/sinistros").then().statusCode(200)
                .extract().jsonPath()
                .getList("findAll { it.descricao == '" + descricao + "' }").size();
    }

    private static String descricaoUnica() {
        return "Idem " + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void mesmaChaveMesmoCorpo_naoDuplicaEDevolveMesmaResposta() {
        String desc = descricaoUnica();
        String chave = UUID.randomUUID().toString();

        Response primeira = post(chave, corpo(desc));
        Response segunda = post(chave, corpo(desc));

        assertEquals(primeira.statusCode(), segunda.statusCode());
        assertNull(primeira.header("Idempotent-Replayed"));
        assertEquals("true", segunda.header("Idempotent-Replayed"));
        assertEquals(primeira.body().asString(), segunda.body().asString());
        assertEquals(1, contar(desc));
    }

    @Test
    void mesmaChaveCorpoDiferente_devolve422() {
        String chave = UUID.randomUUID().toString();

        post(chave, corpo(descricaoUnica()));
        Response segunda = post(chave, corpo(descricaoUnica()));

        assertEquals(422, segunda.statusCode());
    }

    @Test
    void chavesDiferentes_criamSinistrosDiferentes() {
        String desc = descricaoUnica();

        post(UUID.randomUUID().toString(), corpo(desc));
        post(UUID.randomUUID().toString(), corpo(desc));

        assertEquals(2, contar(desc));
    }

    @Test
    void semCabecalho_mantemComportamentoAntigo() {
        String desc = descricaoUnica();

        Response a = post(null, corpo(desc));
        Response b = post(null, corpo(desc));

        assertNull(a.header("Idempotent-Replayed"));
        assertNull(b.header("Idempotent-Replayed"));
        assertEquals(2, contar(desc));
    }
}