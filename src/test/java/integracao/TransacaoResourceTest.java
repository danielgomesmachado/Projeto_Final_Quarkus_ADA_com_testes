package integracao;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import org.acme.entity.*;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Testes de integração do TransacaoResource.
 * Dados são criados diretamente via Panache no @BeforeAll para garantir
 * independência em relação à ordem de execução das suites.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TransacaoResourceTest {

    static Long transacaoId;
    static Long contaId;

    @BeforeAll
    @Transactional
    static void criarDadosBase() {
        // Cria cliente
        Cliente cliente = new Cliente();
        cliente.nome  = "Transacao Teste";
        cliente.cpf   = "777.777.777-77";
        cliente.email = "transacao@teste.com";
        cliente.senha = "senha123";
        cliente.role  = "CLIENTE";
        cliente.persist();

        // Cria conta com saldo
        Conta conta = new Conta();
        conta.numero  = "0099-9";
        conta.tipo    = TipoConta.CORRENTE;
        conta.saldo   = 1000.0;
        conta.cliente = cliente;
        conta.persist();
        contaId = conta.id;

        // Cria transação de depósito
        Transacao tx = new Transacao();
        tx.tipo        = TipoTransacao.DEPOSITO;
        tx.valor       = 1000.0;
        tx.contaOrigem = conta;
        tx.dataHora    = LocalDateTime.now();
        tx.persist();
        transacaoId = tx.id;
    }

    // ─── GET /transacoes/{id} ─────────────────────────────────────────────────

    @Test
    @Order(1)
    @TestSecurity(user = "cliente", roles = "CLIENTE")
    @DisplayName("GET /transacoes/{id} — deve retornar transação existente com 200")
    void buscarTransacaoPorId_deveRetornar200_comBodyCorreto() {
        given()
        .when()
            .get("/transacoes/" + transacaoId)
        .then()
            .statusCode(200)
            .body("id",    equalTo(transacaoId.intValue()))
            .body("tipo",  equalTo("DEPOSITO"))
            .body("valor", equalTo(1000.0f))
            .body("conta.id", notNullValue());
    }

    @Test
    @Order(2)
    @TestSecurity(user = "cliente", roles = "CLIENTE")
    @DisplayName("GET /transacoes/{id} — deve retornar 404 para id inexistente")
    void buscarTransacaoPorId_deveRetornar404_quandoNaoExiste() {
        given()
        .when()
            .get("/transacoes/999999")
        .then()
            .statusCode(404)
            .body("erro", notNullValue());
    }

    // ─── GET /transacoes?contaId= ─────────────────────────────────────────────

    @Test
    @Order(3)
    @TestSecurity(user = "cliente", roles = "CLIENTE")
    @DisplayName("GET /transacoes?contaId= — deve listar transações da conta com 200")
    void listarPorConta_deveRetornar200_comAoMenosUmaTransacao() {
        given()
            .queryParam("contaId", contaId)
        .when()
            .get("/transacoes")
        .then()
            .statusCode(200)
            .body("$",    hasSize(greaterThanOrEqualTo(1)))
            .body("[0].tipo", notNullValue());
    }

    @Test
    @Order(4)
    @TestSecurity(user = "cliente", roles = "CLIENTE")
    @DisplayName("GET /transacoes?contaId= — deve retornar lista vazia para conta sem transações")
    void listarPorConta_deveRetornarListaVazia_quandoContaSemTransacoes() {
        // Cria uma conta nova sem transações
        Long contaSemTxId = criarContaSemTransacoes();

        given()
            .queryParam("contaId", contaSemTxId)
        .when()
            .get("/transacoes")
        .then()
            .statusCode(200)
            .body("$", hasSize(0));
    }

    @Test
    @Order(5)
    @TestSecurity(user = "cliente", roles = "CLIENTE")
    @DisplayName("GET /transacoes — deve retornar 400 quando contaId não informado")
    void listarPorConta_deveRetornar400_quandoContaIdAusente() {
        given()
        .when()
            .get("/transacoes")
        .then()
            .statusCode(400);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    @Transactional
    Long criarContaSemTransacoes() {
        Cliente c = new Cliente();
        c.nome  = "Sem Transacoes";
        c.cpf   = "888.888.888-88";
        c.email = "semtx@teste.com";
        c.senha = "senha123";
        c.role  = "CLIENTE";
        c.persist();

        Conta conta = new Conta();
        conta.numero  = "0088-8";
        conta.tipo    = TipoConta.CORRENTE;
        conta.saldo   = 0.0;
        conta.cliente = c;
        conta.persist();
        return conta.id;
    }
}
