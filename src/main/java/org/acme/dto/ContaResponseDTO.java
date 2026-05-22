package org.acme.dto;

import org.acme.entity.Conta;
import org.acme.entity.Transacao;

import java.util.List;
import java.util.Map;

public class ContaResponseDTO {
    public Long id;
    public String numero;
    public String tipo;
    public Double saldo;
    public TitularDTO titular;
    public List<TransacaoResumoDTO> transacoes;
    public Map<String, String> _links;

    public ContaResponseDTO(Conta conta, List<Transacao> transacoesHoje) {
        this.id = conta.id;
        this.numero = conta.numero;
        this.tipo = conta.tipo.name();
        this.saldo = conta.saldo;
        this.titular = new TitularDTO(conta.cliente.id, conta.cliente.nome, conta.cliente.email);
        this.transacoes = transacoesHoje.stream()
                .map(t -> new TransacaoResumoDTO(t.id, t.tipo.name(), t.valor, t.dataHora.toString()))
                .toList();
        this._links = Map.of("transacoes", "/transacoes?contaId=" + conta.id);
    }

    public record TitularDTO(Long id, String nome, String email) {}
    public record TransacaoResumoDTO(Long id, String tipo, Double valor, String dataHora) {}
}
