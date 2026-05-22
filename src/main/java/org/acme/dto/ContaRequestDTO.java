package org.acme.dto;

import jakarta.validation.constraints.NotNull;
import org.acme.entity.TipoConta;

public class ContaRequestDTO {

    @NotNull(message = "Tipo de conta é obrigatório")
    public TipoConta tipo;

    @NotNull(message = "ID do cliente é obrigatório")
    public Long clienteId;
}
