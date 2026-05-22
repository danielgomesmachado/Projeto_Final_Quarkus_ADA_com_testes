package org.acme.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
public class Conta extends PanacheEntity {

    @Column(unique = true, nullable = false)
    public String numero;

    @NotNull(message = "Tipo de conta é obrigatório")
    @Enumerated(EnumType.STRING)
    public TipoConta tipo;

    public Double saldo = 0.0;

    @NotNull(message = "Cliente é obrigatório")
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    public Cliente cliente;
}
