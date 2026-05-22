package unitario;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.acme.dto.OperacaoDTO;
import org.acme.dto.TransacaoResponseDTO;
import org.acme.dto.TransferenciaDTO;
import org.acme.dto.ContaRequestDTO;
import org.acme.dto.ContaResponseDTO;
import org.acme.entity.*;
import org.acme.service.ContaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class ContaServiceTest {

    @Inject
    ContaService contaService;

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Cliente criarCliente(Long id, String nome) {
        Cliente c = new Cliente();
        c.id    = id;
        c.nome  = nome;
        c.cpf   = "000.000.000-0" + id;
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

    // ─── BeforeEach — ativa os mocks do Panache ────────────────────────────────

    @BeforeEach
    void setUp() {
        PanacheMock.mock(Cliente.class);
        PanacheMock.mock(Conta.class);
        PanacheMock.mock(Transacao.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // criar()
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void criar_deveRetornarContaResponseDTO_quandoDadosValidos() {
        // ARRANGE
        Cliente cliente = criarCliente(1L, "Ana");
        PanacheMock.doReturn(cliente).when(Cliente.class);
        Cliente.findById(1L);

        Mockito.doNothing().when(Mockito.any(Conta.class));
        PanacheMock.doReturn(0L).when(Conta.class);
        Conta.count();

        PanacheMock.doReturn(List.of()).when(Transacao.class);
        Transacao.findByContaIdHoje(anyLong());

        ContaRequestDTO dto = new ContaRequestDTO();
        dto.tipo      = TipoConta.CORRENTE;
        dto.clienteId = 1L;

        // ACT
        ContaResponseDTO response = contaService.criar(dto);

        // ASSERT
        assertNotNull(response);
        assertEquals("CORRENTE", response.tipo);
        assertEquals(0.0, response.saldo);
        assertEquals("Ana", response.titular.nome());
    }

    @Test
    void criar_deveLancarNotFoundException_quandoClienteNaoExiste() {
        // ARRANGE
        PanacheMock.doReturn(null).when(Cliente.class);
        Cliente.findById(99L);

        ContaRequestDTO dto = new ContaRequestDTO();
        dto.tipo      = TipoConta.CORRENTE;
        dto.clienteId = 99L;

        // ACT + ASSERT
        assertThrows(NotFoundException.class, () -> contaService.criar(dto));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // buscarPorId()
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void buscarPorId_deveRetornarConta_quandoExiste() {
        // ARRANGE
        Cliente cliente = criarCliente(1L, "Bruno");
        Conta conta     = criarConta(10L, TipoConta.CORRENTE, 500.0, cliente);

        PanacheMock.doReturn(conta).when(Conta.class);
        Conta.findById(10L);

        PanacheMock.doReturn(List.of()).when(Transacao.class);
        Transacao.findByContaIdHoje(10L);

        // ACT
        ContaResponseDTO response = contaService.buscarPorId(10L);

        // ASSERT
        assertNotNull(response);
        assertEquals(10L, response.id);
        assertEquals(500.0, response.saldo);
        assertEquals("Bruno", response.titular.nome());
    }

    @Test
    void buscarPorId_deveLancarNotFoundException_quandoContaNaoExiste() {
        // ARRANGE
        PanacheMock.doReturn(null).when(Conta.class);
        Conta.findById(999L);

        // ACT + ASSERT
        assertThrows(NotFoundException.class, () -> contaService.buscarPorId(999L));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // depositar()
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void depositar_deveAumentarSaldo_quandoContaCorrenteEValorValido() {
        // ARRANGE
        Cliente cliente = criarCliente(1L, "Carlos");
        Conta conta     = criarConta(1L, TipoConta.CORRENTE, 200.0, cliente);

        PanacheMock.doReturn(conta).when(Conta.class);
        Conta.findById(1L);

        PanacheMock.doNothing().when(Transacao.class);
        Transacao.persist(any(Transacao.class));

        OperacaoDTO dto = new OperacaoDTO();
        dto.valor = 300.0;

        // ACT
        TransacaoResponseDTO response = contaService.depositar(1L, dto);

        // ASSERT
        assertEquals(500.0, conta.saldo);
        assertEquals(500.0, response.saldoAtual);
        assertEquals("DEPOSITO", response.tipo);
    }

    @Test
    void depositar_deveLancarNotFoundException_quandoContaNaoExiste() {
        // ARRANGE
        PanacheMock.doReturn(null).when(Conta.class);
        Conta.findById(99L);

        OperacaoDTO dto = new OperacaoDTO();
        dto.valor = 100.0;

        // ACT + ASSERT
        assertThrows(NotFoundException.class, () -> contaService.depositar(99L, dto));
    }

    @Test
    void depositar_deveLancarBadRequestException_quandoContaEletronica() {
        // ARRANGE
        Cliente cliente = criarCliente(1L, "Diana");
        Conta conta     = criarConta(2L, TipoConta.ELETRONICA, 100.0, cliente);

        PanacheMock.doReturn(conta).when(Conta.class);
        Conta.findById(2L);

        OperacaoDTO dto = new OperacaoDTO();
        dto.valor = 50.0;

        // ACT + ASSERT
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> contaService.depositar(2L, dto));
        assertTrue(ex.getMessage().contains("ELETRONICA"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // sacar()
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void sacar_deveReduzirSaldo_quandoSaldoSuficiente() {
        // ARRANGE
        Cliente cliente = criarCliente(1L, "Eduardo");
        Conta conta     = criarConta(3L, TipoConta.CORRENTE, 1000.0, cliente);

        PanacheMock.doReturn(conta).when(Conta.class);
        Conta.findById(3L);

        PanacheMock.doNothing().when(Transacao.class);
        Transacao.persist(any(Transacao.class));

        OperacaoDTO dto = new OperacaoDTO();
        dto.valor = 400.0;

        // ACT
        TransacaoResponseDTO response = contaService.sacar(3L, dto);

        // ASSERT
        assertEquals(600.0, conta.saldo);
        assertEquals(600.0, response.saldoAtual);
        assertEquals("SAQUE", response.tipo);
    }

    @Test
    void sacar_deveLancarBadRequestException_quandoSaldoInsuficiente() {
        // ARRANGE
        Cliente cliente = criarCliente(1L, "Fernanda");
        Conta conta     = criarConta(4L, TipoConta.CORRENTE, 100.0, cliente);

        PanacheMock.doReturn(conta).when(Conta.class);
        Conta.findById(4L);

        OperacaoDTO dto = new OperacaoDTO();
        dto.valor = 500.0;

        // ACT + ASSERT
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> contaService.sacar(4L, dto));
        assertTrue(ex.getMessage().contains("Saldo insuficiente"));
    }

    @Test
    void sacar_deveLancarBadRequestException_quandoContaEletronica() {
        // ARRANGE
        Cliente cliente = criarCliente(1L, "Gustavo");
        Conta conta     = criarConta(5L, TipoConta.ELETRONICA, 500.0, cliente);

        PanacheMock.doReturn(conta).when(Conta.class);
        Conta.findById(5L);

        OperacaoDTO dto = new OperacaoDTO();
        dto.valor = 100.0;

        // ACT + ASSERT
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> contaService.sacar(5L, dto));
        assertTrue(ex.getMessage().contains("ELETRONICA"));
    }

    @Test
    void sacar_deveLancarNotFoundException_quandoContaNaoExiste() {
        // ARRANGE
        PanacheMock.doReturn(null).when(Conta.class);
        Conta.findById(77L);

        OperacaoDTO dto = new OperacaoDTO();
        dto.valor = 100.0;

        // ACT + ASSERT
        assertThrows(NotFoundException.class, () -> contaService.sacar(77L, dto));
    }

    @Test
    void sacar_deveRegistrarTransacao_quandoSaqueRealizado() {
        // ARRANGE — verifica que o saldo decrementado é exatamente o valor sacado
        Cliente cliente = criarCliente(1L, "Helena");
        Conta conta     = criarConta(6L, TipoConta.CORRENTE, 750.0, cliente);

        PanacheMock.doReturn(conta).when(Conta.class);
        Conta.findById(6L);

        PanacheMock.doNothing().when(Transacao.class);
        Transacao.persist(any(Transacao.class));

        OperacaoDTO dto = new OperacaoDTO();
        dto.valor = 250.0;

        // ACT
        contaService.sacar(6L, dto);

        // ASSERT — saldo deve ter sido decrementado exatamente pelo valor
        assertEquals(500.0, conta.saldo, 0.001);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // transferir()
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void transferir_deveDebitarOrigemECreditarDestino_quandoDadosValidos() {
        // ARRANGE
        Cliente clienteA = criarCliente(1L, "Igor");
        Cliente clienteB = criarCliente(2L, "Julia");
        Conta origem  = criarConta(10L, TipoConta.CORRENTE, 1000.0, clienteA);
        Conta destino = criarConta(20L, TipoConta.CORRENTE, 200.0, clienteB);

        PanacheMock.doReturn(origem).when(Conta.class);
        Conta.findById(10L);
        PanacheMock.doReturn(destino).when(Conta.class);
        Conta.findById(20L);

        PanacheMock.doNothing().when(Transacao.class);
        Transacao.persist(any(Transacao.class));

        TransferenciaDTO dto = new TransferenciaDTO();
        dto.contaDestinoId = 20L;
        dto.valor          = 300.0;

        // ACT
        TransacaoResponseDTO response = contaService.transferir(10L, dto);

        // ASSERT
        assertEquals(700.0, origem.saldo,  0.001);
        assertEquals(500.0, destino.saldo, 0.001);
        assertEquals(700.0, response.saldoAtual, 0.001);
        assertEquals("TRANSFERENCIA", response.tipo);
    }

    @Test
    void transferir_deveLancarNotFoundException_quandoContaOrigemNaoExiste() {
        // ARRANGE
        PanacheMock.doReturn(null).when(Conta.class);
        Conta.findById(55L);

        TransferenciaDTO dto = new TransferenciaDTO();
        dto.contaDestinoId = 20L;
        dto.valor          = 100.0;

        // ACT + ASSERT
        assertThrows(NotFoundException.class, () -> contaService.transferir(55L, dto));
    }

    @Test
    void transferir_deveLancarNotFoundException_quandoContaDestinoNaoExiste() {
        // ARRANGE
        Cliente clienteA = criarCliente(1L, "Karla");
        Conta origem = criarConta(10L, TipoConta.CORRENTE, 500.0, clienteA);

        PanacheMock.doReturn(origem).when(Conta.class);
        Conta.findById(10L);

        PanacheMock.doReturn(null).when(Conta.class);
        Conta.findById(66L);

        TransferenciaDTO dto = new TransferenciaDTO();
        dto.contaDestinoId = 66L;
        dto.valor          = 100.0;

        // ACT + ASSERT
        assertThrows(NotFoundException.class, () -> contaService.transferir(10L, dto));
    }

    @Test
    void transferir_deveLancarBadRequestException_quandoSaldoInsuficiente() {
        // ARRANGE
        Cliente clienteA = criarCliente(1L, "Leonardo");
        Cliente clienteB = criarCliente(2L, "Marina");
        Conta origem  = criarConta(10L, TipoConta.CORRENTE, 50.0, clienteA);
        Conta destino = criarConta(20L, TipoConta.CORRENTE, 100.0, clienteB);

        PanacheMock.doReturn(origem).when(Conta.class);
        Conta.findById(10L);
        PanacheMock.doReturn(destino).when(Conta.class);
        Conta.findById(20L);

        TransferenciaDTO dto = new TransferenciaDTO();
        dto.contaDestinoId = 20L;
        dto.valor          = 200.0;

        // ACT + ASSERT
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> contaService.transferir(10L, dto));
        assertTrue(ex.getMessage().contains("Saldo insuficiente"));
    }
}
