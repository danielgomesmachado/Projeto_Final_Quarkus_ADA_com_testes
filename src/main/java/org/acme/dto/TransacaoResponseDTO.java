package org.acme.dto;

import org.acme.entity.Transacao;

public class TransacaoResponseDTO {
    public Long id;
    public String tipo;
    public Double valor;
    public Double saldoAtual;
    public String dataHora;
    public ContaResumoDTO conta;
    public ContaResumoDTO contaDestino;

    public TransacaoResponseDTO(Transacao t, Double saldoAtual) {
        this.id = t.id;
        this.tipo = t.tipo.name();
        this.valor = t.valor;
        this.saldoAtual = saldoAtual;
        this.dataHora = t.dataHora.toString();

        if (t.contaOrigem != null) {
            this.conta = new ContaResumoDTO(
                t.contaOrigem.id,
                t.contaOrigem.numero,
                t.contaOrigem.tipo.name(),
                new TitularResumoDTO(t.contaOrigem.cliente.id, t.contaOrigem.cliente.nome, t.contaOrigem.cliente.email)
            );
        }

        if (t.contaDestino != null) {
            this.contaDestino = new ContaResumoDTO(
                t.contaDestino.id,
                t.contaDestino.numero,
                t.contaDestino.tipo.name(),
                new TitularResumoDTO(t.contaDestino.cliente.id, t.contaDestino.cliente.nome, t.contaDestino.cliente.email)
            );
        }
    }

    public record TitularResumoDTO(Long id, String nome, String email) {}
    public record ContaResumoDTO(Long id, String numero, String tipo, TitularResumoDTO titular) {}
}
