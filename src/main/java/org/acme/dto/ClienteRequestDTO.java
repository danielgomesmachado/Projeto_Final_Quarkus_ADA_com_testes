package org.acme.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.acme.entity.Cliente;

// ─── Cliente ───────────────────────────────────────────────

public class ClienteRequestDTO {

    @NotBlank(message = "Nome é obrigatório")
    public String nome;

    // CPF só é obrigatório no POST — no PUT sua presença dispara erro 400
    public String cpf;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    public String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 3, message = "Senha deve ter ao menos 3 caracteres")
    public String senha;
}
