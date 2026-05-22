package org.acme.dto;

import org.acme.entity.Cliente;

public class ClienteResponseDTO {
    public Long id;
    public String nome;
    public String email;

    public ClienteResponseDTO(Cliente cliente) {
        this.id = cliente.id;
        this.nome = cliente.nome;
        this.email = cliente.email;
    }
}
