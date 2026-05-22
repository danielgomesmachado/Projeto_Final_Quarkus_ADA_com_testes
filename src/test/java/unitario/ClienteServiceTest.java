package unitario;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.acme.dto.ClienteRequestDTO;
import org.acme.dto.ClienteResponseDTO;
import org.acme.entity.Cliente;
import org.acme.service.ClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ClienteServiceTest {

    @Inject
    ClienteService clienteService;

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Cliente criarCliente(Long id, String nome, String cpf, String email) {
        Cliente c = new Cliente();
        c.id    = id;
        c.nome  = nome;
        c.cpf   = cpf;
        c.email = email;
        c.senha = "senha123";
        c.role  = "CLIENTE";
        return c;
    }

    @BeforeEach
    void setUp() {
        PanacheMock.mock(Cliente.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // listarTodos()
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void listarTodos_deveRetornarListaVazia_quandoNaoHaClientes() {
        // ARRANGE
        PanacheMock.doReturn(List.of()).when(Cliente.class);
        Cliente.listAll();

        // ACT
        List<ClienteResponseDTO> resultado = clienteService.listarTodos();

        // ASSERT
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void listarTodos_deveRetornarTodosClientes_quandoExistem() {
        // ARRANGE
        Cliente c1 = criarCliente(1L, "Alice", "111.111.111-11", "alice@email.com");
        Cliente c2 = criarCliente(2L, "Bob",   "222.222.222-22", "bob@email.com");

        PanacheMock.doReturn(List.of(c1, c2)).when(Cliente.class);
        Cliente.listAll();

        // ACT
        List<ClienteResponseDTO> resultado = clienteService.listarTodos();

        // ASSERT
        assertEquals(2, resultado.size());
        assertEquals("Alice", resultado.get(0).nome);
        assertEquals("Bob",   resultado.get(1).nome);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // buscarPorId()
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void buscarPorId_deveRetornarCliente_quandoExiste() {
        // ARRANGE
        Cliente cliente = criarCliente(1L, "Clara", "333.333.333-33", "clara@email.com");

        PanacheMock.doReturn(cliente).when(Cliente.class);
        Cliente.findById(1L);

        // ACT
        ClienteResponseDTO response = clienteService.buscarPorId(1L);

        // ASSERT
        assertNotNull(response);
        assertEquals(1L,              response.id);
        assertEquals("Clara",         response.nome);
        assertEquals("clara@email.com", response.email);
    }

    @Test
    void buscarPorId_deveLancarNotFoundException_quandoClienteNaoExiste() {
        // ARRANGE
        PanacheMock.doReturn(null).when(Cliente.class);
        Cliente.findById(999L);

        // ACT + ASSERT
        assertThrows(NotFoundException.class, () -> clienteService.buscarPorId(999L));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // criar()
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void criar_deveRetornarClienteResponseDTO_quandoDadosValidos() {
        // ARRANGE
        PanacheMock.doNothing().when(Cliente.class);
        // persist() é void — nenhuma configuração extra necessária

        ClienteRequestDTO dto = new ClienteRequestDTO();
        dto.nome  = "David";
        dto.cpf   = "444.444.444-44";
        dto.email = "david@email.com";
        dto.senha = "senha123";

        // ACT
        ClienteResponseDTO response = clienteService.criar(dto);

        // ASSERT
        assertNotNull(response);
        assertEquals("David",           response.nome);
        assertEquals("david@email.com", response.email);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // atualizar()
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void atualizar_deveAtualizarNomeEEmail_quandoDadosValidos() {
        // ARRANGE
        Cliente cliente = criarCliente(1L, "Eva", "555.555.555-55", "eva@email.com");

        PanacheMock.doReturn(cliente).when(Cliente.class);
        Cliente.findById(1L);

        ClienteRequestDTO dto = new ClienteRequestDTO();
        dto.nome  = "Eva Sobrenome";
        dto.email = "eva.novo@email.com";
        dto.senha = "novaSenha";

        // ACT
        ClienteResponseDTO response = clienteService.atualizar(1L, dto);

        // ASSERT
        assertEquals("Eva Sobrenome",      response.nome);
        assertEquals("eva.novo@email.com", response.email);
    }

    @Test
    void atualizar_deveLancarBadRequestException_quandoCpfEnviado() {
        // ARRANGE
        ClienteRequestDTO dto = new ClienteRequestDTO();
        dto.nome  = "Felipe";
        dto.cpf   = "666.666.666-66";   // CPF presente → deve lançar erro
        dto.email = "felipe@email.com";
        dto.senha = "senha123";

        // ACT + ASSERT
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> clienteService.atualizar(1L, dto));
        assertTrue(ex.getMessage().contains("CPF"));
    }

    @Test
    void atualizar_deveLancarNotFoundException_quandoClienteNaoExiste() {
        // ARRANGE
        ClienteRequestDTO dto = new ClienteRequestDTO();
        dto.nome  = "Gio";
        dto.email = "gio@email.com";
        dto.senha = "senha123";

        PanacheMock.doReturn(null).when(Cliente.class);
        Cliente.findById(88L);

        // ACT + ASSERT
        assertThrows(NotFoundException.class, () -> clienteService.atualizar(88L, dto));
    }
}
