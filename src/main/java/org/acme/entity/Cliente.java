package org.acme.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
public class Cliente extends PanacheEntity {

    @NotBlank(message = "Nome é obrigatório")
    public String nome;

    @NotBlank(message = "CPF é obrigatório")
    @Column(unique = true, nullable = false)
    public String cpf;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Column(unique = true, nullable = false)
    public String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 3, message = "Senha deve ter ao menos 3 caracteres")
    public String senha;

    public String role = "CLIENTE";
}
