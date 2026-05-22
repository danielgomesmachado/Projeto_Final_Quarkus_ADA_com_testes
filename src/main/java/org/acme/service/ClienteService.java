package org.acme.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.acme.dto.ClienteRequestDTO;
import org.acme.dto.ClienteResponseDTO;
import org.acme.entity.Cliente;

import java.util.List;

@ApplicationScoped
public class ClienteService {

    public List<ClienteResponseDTO> listarTodos() {
        return Cliente.<Cliente>listAll()
                .stream()
                .map(ClienteResponseDTO::new)
                .toList();
    }

    public ClienteResponseDTO buscarPorId(Long id) {
        Cliente cliente = Cliente.findById(id);
        if (cliente == null) {
            throw new NotFoundException("Cliente não encontrado com id: " + id);
        }
        return new ClienteResponseDTO(cliente);
    }

    @Transactional
    public ClienteResponseDTO criar(ClienteRequestDTO dto) {
        Cliente cliente = new Cliente();
        cliente.nome = dto.nome;
        cliente.cpf = dto.cpf;
        cliente.email = dto.email;
        cliente.senha = dto.senha;
        cliente.role = "CLIENTE";
        cliente.persist();
        return new ClienteResponseDTO(cliente);
    }

    @Transactional
    public ClienteResponseDTO atualizar(Long id, ClienteRequestDTO dto) {
        if (dto.cpf != null) {
            throw new BadRequestException("CPF não pode ser atualizado.");
        }

        Cliente cliente = Cliente.findById(id);
        if (cliente == null) {
            throw new NotFoundException("Cliente não encontrado com id: " + id);
        }

        if (dto.nome != null) cliente.nome = dto.nome;
        if (dto.email != null) cliente.email = dto.email;
        if (dto.senha != null) cliente.senha = dto.senha;

        return new ClienteResponseDTO(cliente);
    }
}
