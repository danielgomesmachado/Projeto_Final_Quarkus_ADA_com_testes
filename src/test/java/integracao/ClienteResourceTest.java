package integracao;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import org.acme.entity.Conta;
import org.acme.entity.Cliente;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Testes de integração do ClienteResource.
 * Fluxo completo sem mocks — banco H2 em memória.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClienteResourceTest {

    static Long clienteId;

    @BeforeAll
    @Transactional
    static void limparBase() {
        Conta.deleteAll();
        Cliente.deleteAll();
    }

    // ─── POST /clientes ───────────────────────────────────────────────────────

    @Test
    @Order(1)
    @TestSecurity(user = "gerente", roles = "GERENTE")
    @DisplayName("POST /clientes — deve criar cliente e retornar 201 com body correto")
    void criarCliente_deveRetornar201_comBodyCorreto() {
        String body = """
            {
                "nome":  "Ana Teste",
                "cpf":   "123.456.789-00",
                "email": "ana@teste.com",
                "senha": "senha123"
            }
            """;

        clienteId =
            given()
                .contentType(ContentType.JSON)
                .body(body)
            .when()
                .post("/clientes")
            .then()
                .statusCode(201)
                .body("id",    notNullValue())
                .body("nome",  equalTo("Ana Teste"))
                .body("email", equalTo("ana@teste.com"))
            .extract()
                .jsonPath().getLong("id");
    }

    @Test
    @Order(2)
    @TestSecurity(user = "gerente", roles = "GERENTE")
    @DisplayName("POST /clientes — deve retornar 400 quando nome está ausente")
    void criarCliente_deveRetornar400_quandoNomeAusente() {
        String body = """
            {
                "cpf":   "000.000.000-99",
                "email": "sem.nome@teste.com",
                "senha": "senha123"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/clientes")
        .then()
            .statusCode(400);
    }

    @Test
    @Order(3)
    @TestSecurity(user = "gerente", roles = "GERENTE")
    @DisplayName("POST /clientes — deve retornar 400 para email inválido")
    void criarCliente_deveRetornar400_quandoEmailInvalido() {
        String body = """
            {
                "nome":  "Email Errado",
                "cpf":   "000.000.000-98",
                "email": "nao-e-um-email",
                "senha": "senha123"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/clientes")
        .then()
            .statusCode(400);
    }

    @Test
    @Order(4)
    @TestSecurity(user = "gerente", roles = "GERENTE")
    @DisplayName("POST /clientes — deve retornar 400 para senha muito curta")
    void criarCliente_deveRetornar400_quandoSenhaCurta() {
        String body = """
            {
                "nome":  "Senha Curta",
                "cpf":   "000.000.000-97",
                "email": "senha.curta@teste.com",
                "senha": "ab"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/clientes")
        .then()
            .statusCode(400);
    }

    // ─── GET /clientes ────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @TestSecurity(user = "gerente", roles = "GERENTE")
    @DisplayName("GET /clientes — deve retornar lista com ao menos o cliente criado")
    void listarClientes_deveRetornar200_comAoMenosUmItem() {
        given()
        .when()
            .get("/clientes")
        .then()
            .statusCode(200)
            .body("$",    hasSize(greaterThanOrEqualTo(1)))
            .body("[0].nome", notNullValue());
    }

    // ─── GET /clientes/{id} ───────────────────────────────────────────────────

    @Test
    @Order(6)
    @TestSecurity(user = "gerente", roles = "GERENTE")
    @DisplayName("GET /clientes/{id} — deve retornar cliente existente com 200")
    void buscarClientePorId_deveRetornar200_comCamposCorretos() {
        given()
        .when()
            .get("/clientes/" + clienteId)
        .then()
            .statusCode(200)
            .body("id",    equalTo(clienteId.intValue()))
            .body("nome",  equalTo("Ana Teste"))
            .body("email", equalTo("ana@teste.com"));
    }

    @Test
    @Order(7)
    @TestSecurity(user = "gerente", roles = "GERENTE")
    @DisplayName("GET /clientes/{id} — deve retornar 404 para id inexistente")
    void buscarClientePorId_deveRetornar404_quandoNaoExiste() {
        given()
        .when()
            .get("/clientes/999999")
        .then()
            .statusCode(404)
            .body("erro", notNullValue());
    }

    // ─── PUT /clientes/{id} ───────────────────────────────────────────────────

    @Test
    @Order(8)
    @TestSecurity(user = "gerente", roles = "GERENTE")
    @DisplayName("PUT /clientes/{id} — deve atualizar nome e email com 200")
    void atualizarCliente_deveRetornar200_comDadosAtualizados() {
        String body = """
            {
                "nome":  "Ana Atualizada",
                "email": "ana.nova@teste.com",
                "senha": "novaSenha123"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .put("/clientes/" + clienteId)
        .then()
            .statusCode(200)
            .body("nome",  equalTo("Ana Atualizada"))
            .body("email", equalTo("ana.nova@teste.com"));
    }

    @Test
    @Order(9)
    @TestSecurity(user = "gerente", roles = "GERENTE")
    @DisplayName("PUT /clientes/{id} — deve retornar 400 ao tentar atualizar CPF")
    void atualizarCliente_deveRetornar400_quandoTentaAlterarCpf() {
        String body = """
            {
                "nome": "Ana CPF",
                "cpf":  "999.999.999-99",
                "email": "ana@teste.com",
                "senha": "senha123"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .put("/clientes/" + clienteId)
        .then()
            .statusCode(400)
            .body("erro", containsString("CPF"));
    }

    @Test
    @Order(10)
    @TestSecurity(user = "gerente", roles = "GERENTE")
    @DisplayName("PUT /clientes/{id} — deve retornar 404 para cliente inexistente")
    void atualizarCliente_deveRetornar404_quandoNaoExiste() {
        String body = """
            {
                "nome":  "Fantasma",
                "email": "fantasma@teste.com",
                "senha": "senha123"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .put("/clientes/999999")
        .then()
            .statusCode(404);
    }
}
