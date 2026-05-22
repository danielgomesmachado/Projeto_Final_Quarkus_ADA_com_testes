package org.acme.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class TransferenciaDTO {

    @NotNull(message = "Conta de destino é obrigatória")
    public Long contaDestinoId;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    public Double valor;
}
