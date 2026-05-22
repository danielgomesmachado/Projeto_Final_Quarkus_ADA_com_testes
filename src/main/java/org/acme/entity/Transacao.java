package org.acme.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Transacao extends PanacheEntity {

    @NotNull(message = "Tipo de transação é obrigatório")
    @Enumerated(EnumType.STRING)
    public TipoTransacao tipo;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    public Double valor;

    public LocalDateTime dataHora = LocalDateTime.now();

    @NotNull
    @ManyToOne
    @JoinColumn(name = "conta_origem_id")
    public Conta contaOrigem;

    @ManyToOne
    @JoinColumn(name = "conta_destino_id")
    public Conta contaDestino;

    public static List<Transacao> findByContaId(Long contaId) {
        return list("contaOrigem.id = ?1 or contaDestino.id = ?1", contaId);
    }

    public static List<Transacao> findByContaIdHoje(Long contaId) {
        LocalDateTime inicioDia = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime fimDia = inicioDia.plusDays(1);
        return list("(contaOrigem.id = ?1 or contaDestino.id = ?1) and dataHora >= ?2 and dataHora < ?3",
                contaId, inicioDia, fimDia);
    }
}
