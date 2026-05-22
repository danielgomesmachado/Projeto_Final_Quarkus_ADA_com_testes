package unitario;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.acme.dto.TransacaoResponseDTO;
import org.acme.entity.*;
import org.acme.service.TransacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TransacaoServiceTest {

    @Inject
    TransacaoService transacaoService;

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Cliente criarCliente(Long id, String nome) {
        Cliente c = new Cliente();
        c.id    = id;
        c.nome  = nome;
        c.cpf   = "00" + id + ".000.000-00";
        c.email = nome.toLowerCase() + "@email.com";
        c.senha = "senha123";
        c.role  = "CLIENTE";
        return c;
    }

    private Conta criarConta(Long id, TipoConta tipo, Double saldo, Cliente cliente) {
        Conta c = new Conta();
        c.id      = id;
        c.numero  = "000" + id + "-" + (id % 10);
        c.tipo    = tipo;
        c.saldo   = saldo;
        c.cliente = cliente;
        return c;
    }

    private Transacao criarTransacao(Long id, TipoTransacao tipo, Double valor, Conta conta) {
        Transacao t = new Transacao();
        t.id           = id;
        t.tipo         = tipo;
        t.valor        = valor;
        t.contaOrigem  = conta;
        t.dataHora     = LocalDateTime.now();
        return t;
    }

    @BeforeEach
    void setUp() {
        PanacheMock.mock(Transacao.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // buscarPorId()
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void buscarPorId_deveRetornarTransacao_quandoExiste() {
        // ARRANGE
        Cliente cliente = criarCliente(1L, "Ana");
        Conta conta     = criarConta(10L, TipoConta.CORRENTE, 800.0, cliente);
        Transacao tx    = criarTransacao(100L, TipoTransacao.DEPOSITO, 200.0, conta);

        PanacheMock.doReturn(tx).when(Transacao.class);
        Transacao.findById(100L);

        // ACT
        TransacaoResponseDTO response = transacaoService.buscarPorId(100L);

        // ASSERT
        assertNotNull(response);
        assertEquals(100L,     response.id);
        assertEquals("DEPOSITO", response.tipo);
        assertEquals(200.0,    response.valor);
        assertNull(response.saldoAtual); // buscarPorId passa null para saldoAtual
    }

    @Test
    void buscarPorId_deveLancarNotFoundException_quandoNaoExiste() {
        // ARRANGE
        PanacheMock.doReturn(null).when(Transacao.class);
        Transacao.findById(999L);

        // ACT + ASSERT
        assertThrows(NotFoundException.class, () -> transacaoService.buscarPorId(999L));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // listarPorConta()
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void listarPorConta_deveRetornarListaVazia_quandoNaoHaTransacoes() {
        // ARRANGE
        PanacheMock.doReturn(List.of()).when(Transacao.class);
        Transacao.findByContaId(10L);

        // ACT
        List<TransacaoResponseDTO> resultado = transacaoService.listarPorConta(10L);

        // ASSERT
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void listarPorConta_deveRetornarTodasTransacoesDaConta_quandoExistem() {
        // ARRANGE
        Cliente cliente = criarCliente(1L, "Bruno");
        Conta conta     = criarConta(10L, TipoConta.CORRENTE, 1000.0, cliente);

        Transacao tx1 = criarTransacao(1L, TipoTransacao.DEPOSITO,      500.0, conta);
        Transacao tx2 = criarTransacao(2L, TipoTransacao.SAQUE,         200.0, conta);
        Transacao tx3 = criarTransacao(3L, TipoTransacao.TRANSFERENCIA, 100.0, conta);

        PanacheMock.doReturn(List.of(tx1, tx2, tx3)).when(Transacao.class);
        Transacao.findByContaId(10L);

        // ACT
        List<TransacaoResponseDTO> resultado = transacaoService.listarPorConta(10L);

        // ASSERT
        assertEquals(3, resultado.size());
        assertEquals("DEPOSITO",      resultado.get(0).tipo);
        assertEquals("SAQUE",         resultado.get(1).tipo);
        assertEquals("TRANSFERENCIA", resultado.get(2).tipo);
    }
}
