package integracao;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.entity.Cliente;
import org.acme.entity.Conta;
import org.acme.entity.TipoConta;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Testes de integração do ContaResource.
 * Fluxo completo: HTTP → Resource → Service → Repository → H2 → resposta HTTP.
 * Sem mocks — tudo real.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContaResourceTest {

    // IDs criados nos testes — compartilhados entre métodos da suite
    static Long clienteId;
    static Long contaCorrenteId;
    static Long contaEletronicaId;
    static Long contaDestinoId;

    // ─── Setup: cria dados base antes de todos os testes ──────────────────────

    @BeforeAll
    @Transactional
    static void criarDadosBase() {
        // Limpa dados anteriores
        Conta.deleteAll();
        Cliente.deleteAll();

        // Cliente titular das contas de teste
        Cliente c = new Cliente();
        c.nome  = "Integração Teste";
        c.cpf   = "000.000.000-01";
        c.email = "integracao@teste.com";
        c.senha = "senha123";
        c.role  = "GERENTE";
        c.persist();
        clienteId = c.id;
    }

    // ─── POST /contas ──────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @TestSecurity(user = "gerente", roles = "GERENTE")
    @DisplayName("POST /contas — deve criar conta CORRENTE e retornar 201")
    void criarContaCorrente_deveRetornar201_comBodyCorreto() {
        String body = """
            {
                "tipo": "CORRENTE",
                "clienteId": %d
            }
            """.formatted(clienteId);

        contaCorrenteId =
            given()
                .contentType(ContentType.JSON)
                .body(body)
            .when()
                .post("/contas")
            .then()
                .statusCode(201)
                .body("id",     notNullValue())
                .body("tipo",   equalTo("CORRENTE"))
                .body("saldo",  equalTo(0.0f))
                .body("numero", notNullValue())
                .body("titular.nome", equalTo("Integração Teste"))
            .extract()
                .jsonPath().getLong("id");
    }

    @Test
    @Order(2)
    @TestSecurity(user = "gerente", roles = "GERENTE")
    @DisplayName("POST /contas — deve criar conta ELETRONICA e retornar 201")
    void criarContaEletronica_deveRetornar201() {
        String body = """
            {
                "tipo": "ELETRONICA",
                "clienteId": %d
            }
            """.formatted(clienteId);

        contaEletronicaId =
            given()
                .contentType(ContentType.JSON)
                .body(body)
            .when()
                .post("/contas")
            .then()
                .statusCode(201)
                .body("tipo", equalTo("ELETRONICA"))
            .extract()
                .jsonPath().getLong("id");
    }

    @Test
    @Order(3)
    @TestSecurity(user = "gerente", roles = "GERENTE")
    @DisplayName("POST /contas — deve criar conta destino para transferências")
    void criarContaDestino_deveRetornar201() {
        // Cria segundo cliente para a conta destino
        String clienteBody = """
            {
                "nome":  "Destino Transferencia",
                "cpf":   "111.111.111-11",
                "email": "destino@teste.com",
                "senha": "senha123"
            }
            """;

        Long destClienteId =
            given()
                .contentType(ContentType.JSON)
                .body(clienteBody)
            .when()
                .post("/clientes")
            .then()
                .statusCode(201)
            .extract()
                .jsonPath().getLong("id");

        String contaBody = """
            {
                "tipo": "CORRENTE",
                "clienteId": %d
            }
            """.formatted(destClienteId);

        contaDestinoId =
            given()
                .contentType(ContentType.JSON)
                .body(contaBody)
            .when()
                .post("/contas")
            .then()
                .statusCode(201)
            .extract()
                .jsonPath().getLong("id");
    }

    @Test
    @Order(4)
    @TestSecurity(user = "gerente", roles = "GERENTE")
    @DisplayName("POST /contas — deve retornar 404 quando clienteId não existe")
    void criarConta_deveRetornar404_quandoClienteInexistente() {
        String body = """
            {
                "tipo": "CORRENTE",
                "clienteId": 99999
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/contas")
        .then()
            .statusCode(404);
    }

    @Test
    @Order(5)
    @TestSecurity(user = "gerente", roles = "GERENTE")
    @DisplayName("POST /contas — deve retornar 400 quando tipo está ausente")
    void criarConta_deveRetornar400_quandoTipoAusente() {
        String body = """
            {
                "clienteId": %d
            }
            """.formatted(clienteId);

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/contas")
        .then()
            .statusCode(400);
    }

    // ─── GET /contas/{id} ─────────────────────────────────────────────────────

    @Test
    @Order(6)
    @TestSecurity(user = "cliente", roles = "CLIENTE")
    @DisplayName("GET /contas/{id} — deve retornar conta existente com 200")
    void buscarContaPorId_deveRetornar200_comCamposCorretos() {
        given()
        .when()
            .get("/contas/" + contaCorrenteId)
        .then()
            .statusCode(200)
            .body("id",    equalTo(contaCorrenteId.intValue()))
            .body("tipo",  equalTo("CORRENTE"))
            .body("saldo", equalTo(0.0f))
            .body("titular.nome", equalTo("Integração Teste"))
            .body("_links.transacoes", containsString("/transacoes?contaId="));
    }

    @Test
    @Order(7)
    @TestSecurity(user = "cliente", roles = "CLIENTE")
    @DisplayName("GET /contas/{id} — deve retornar 404 para id inexistente")
    void buscarContaPorId_deveRetornar404_quandoNaoExiste() {
        given()
        .when()
            .get("/contas/999999")
        .then()
            .statusCode(404)
            .body("erro", notNullValue());
    }

    // ─── POST /contas/{id}/deposito ───────────────────────────────────────────

    @Test
    @Order(8)
    @TestSecurity(user = "cliente", roles = "CLIENTE")
    @DisplayName("POST /contas/{id}/deposito — deve depositar e retornar saldo atualizado")
    void depositar_deveRetornar200_comSaldoAtualizado() {
        String body = """
            { "valor": 500.0 }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/contas/" + contaCorrenteId + "/deposito")
        .then()
            .statusCode(200)
            .body("tipo",        equalTo("DEPOSITO"))
            .body("valor",       equalTo(500.0f))
            .body("saldoAtual",  equalTo(500.0f));
    }

    @Test
    @Order(9)
    @TestSecurity(user = "cliente", roles = "CLIENTE")
    @DisplayName("POST /contas/{id}/deposito — deve retornar 400 para conta ELETRONICA")
    void depositar_deveRetornar400_quandoContaEletronica() {
        String body = """
            { "valor": 100.0 }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/contas/" + contaEletronicaId + "/deposito")
        .then()
            .statusCode(400)
            .body("erro", containsString("ELETRONICA"));
    }

    @Test
    @Order(10)
    @TestSecurity(user = "cliente", roles = "CLIENTE")
    @DisplayName("POST /contas/{id}/deposito — deve retornar 400 para valor zero")
    void depositar_deveRetornar400_quandoValorZero() {
        String body = """
            { "valor": 0.0 }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/contas/" + contaCorrenteId + "/deposito")
        .then()
            .statusCode(400);
    }

    @Test
    @Order(11)
    @TestSecurity(user = "cliente", roles = "CLIENTE")
    @DisplayName("POST /contas/{id}/deposito — deve retornar 404 para conta inexistente")
    void depositar_deveRetornar404_quandoContaNaoExiste() {
        String body = """
            { "valor": 100.0 }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/contas/999999/deposito")
        .then()
            .statusCode(404);
    }

    // ─── POST /contas/{id}/saque ──────────────────────────────────────────────

    @Test
    @Order(12)
    @TestSecurity(user = "cliente", roles = "CLIENTE")
    @DisplayName("POST /contas/{id}/saque — deve sacar e retornar saldo atualizado")
    void sacar_deveRetornar200_comSaldoAtualizado() {
        // Saldo atual é 500 (depositado no teste anterior)
        String body = """
            { "valor": 200.0 }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/contas/" + contaCorrenteId + "/saque")
        .then()
            .statusCode(200)
            .body("tipo",        equalTo("SAQUE"))
            .body("valor",       equalTo(200.0f))
            .body("saldoAtual",  equalTo(300.0f));
    }

    @Test
    @Order(13)
    @TestSecurity(user = "cliente", roles = "CLIENTE")
    @DisplayName("POST /contas/{id}/saque — deve retornar 400 por saldo insuficiente")
    void sacar_deveRetornar400_quandoSaldoInsuficiente() {
        String body = """
            { "valor": 99999.0 }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/contas/" + contaCorrenteId + "/saque")
        .then()
            .statusCode(400)
            .body("erro", containsString("Saldo insuficiente"));
    }

    @Test
    @Order(14)
    @TestSecurity(user = "cliente", roles = "CLIENTE")
    @DisplayName("POST /contas/{id}/saque — deve retornar 400 para conta ELETRONICA")
    void sacar_deveRetornar400_quandoContaEletronica() {
        String body = """
            { "valor": 50.0 }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/contas/" + contaEletronicaId + "/saque")
        .then()
            .statusCode(400)
            .body("erro", containsString("ELETRONICA"));
    }

    // ─── POST /contas/{id}/transferencia ──────────────────────────────────────

    @Test
    @Order(15)
    @TestSecurity(user = "cliente", roles = "CLIENTE")
    @DisplayName("POST /contas/{id}/transferencia — deve transferir e retornar 200")
    void transferir_deveRetornar200_comSaldosAtualizados() {
        // Saldo origem é 300 após depósito e saque anteriores
        String body = """
            {
                "contaDestinoId": %d,
                "valor": 100.0
            }
            """.formatted(contaDestinoId);

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/contas/" + contaCorrenteId + "/transferencia")
        .then()
            .statusCode(200)
            .body("tipo",       equalTo("TRANSFERENCIA"))
            .body("valor",      equalTo(100.0f))
            .body("saldoAtual", equalTo(200.0f));
    }

    @Test
    @Order(16)
    @TestSecurity(user = "cliente", roles = "CLIENTE")
    @DisplayName("POST /contas/{id}/transferencia — deve retornar 400 por saldo insuficiente")
    void transferir_deveRetornar400_quandoSaldoInsuficiente() {
        String body = """
            {
                "contaDestinoId": %d,
                "valor": 99999.0
            }
            """.formatted(contaDestinoId);

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/contas/" + contaCorrenteId + "/transferencia")
        .then()
            .statusCode(400)
            .body("erro", containsString("Saldo insuficiente"));
    }

    @Test
    @Order(17)
    @TestSecurity(user = "cliente", roles = "CLIENTE")
    @DisplayName("POST /contas/{id}/transferencia — deve retornar 404 conta destino inexistente")
    void transferir_deveRetornar404_quandoContaDestinoNaoExiste() {
        String body = """
            {
                "contaDestinoId": 999999,
                "valor": 10.0
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/contas/" + contaCorrenteId + "/transferencia")
        .then()
            .statusCode(404);
    }
}
