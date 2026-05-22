package org.acme.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.acme.dto.*;
import org.acme.entity.*;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ContaService {

    @Transactional
    public ContaResponseDTO criar(ContaRequestDTO dto) {
        Cliente cliente = Cliente.findById(dto.clienteId);
        if (cliente == null) {
            throw new NotFoundException("Cliente não encontrado com id: " + dto.clienteId);
        }

        Conta conta = new Conta();
        conta.tipo = dto.tipo;
        conta.cliente = cliente;
        conta.saldo = 0.0;
        conta.numero = gerarNumeroConta();
        conta.persist();

        return new ContaResponseDTO(conta, List.of());
    }

    public ContaResponseDTO buscarPorId(Long id) {
        Conta conta = Conta.findById(id);
        if (conta == null) {
            throw new NotFoundException("Conta não encontrada com id: " + id);
        }
        List<Transacao> transacoesHoje = Transacao.findByContaIdHoje(id);
        return new ContaResponseDTO(conta, transacoesHoje);
    }

    @Transactional
    public TransacaoResponseDTO depositar(Long contaId, OperacaoDTO dto) {
        Conta conta = Conta.findById(contaId);
        if (conta == null) {
            throw new NotFoundException("Conta não encontrada com id: " + contaId);
        }
        if (conta.tipo == TipoConta.ELETRONICA) {
            throw new BadRequestException("Conta do tipo ELETRONICA não permite depósitos.");
        }

        conta.saldo += dto.valor;

        Transacao transacao = new Transacao();
        transacao.tipo = TipoTransacao.DEPOSITO;
        transacao.valor = dto.valor;
        transacao.contaOrigem = conta;
        transacao.dataHora = LocalDateTime.now();
        transacao.persist();

        return new TransacaoResponseDTO(transacao, conta.saldo);
    }

    @Transactional
    public TransacaoResponseDTO sacar(Long contaId, OperacaoDTO dto) {
        Conta conta = Conta.findById(contaId);
        if (conta == null) {
            throw new NotFoundException("Conta não encontrada com id: " + contaId);
        }
        if (conta.tipo == TipoConta.ELETRONICA) {
            throw new BadRequestException("Conta do tipo ELETRONICA não permite saques.");
        }
        if (conta.saldo < dto.valor) {
            throw new BadRequestException("Saldo insuficiente para realizar o saque.");
        }

        conta.saldo -= dto.valor;

        Transacao transacao = new Transacao();
        transacao.tipo = TipoTransacao.SAQUE;
        transacao.valor = dto.valor;
        transacao.contaOrigem = conta;
        transacao.dataHora = LocalDateTime.now();
        transacao.persist();

        return new TransacaoResponseDTO(transacao, conta.saldo);
    }

    @Transactional
    public TransacaoResponseDTO transferir(Long contaOrigemId, TransferenciaDTO dto) {
        Conta origem = Conta.findById(contaOrigemId);
        if (origem == null) {
            throw new NotFoundException("Conta de origem não encontrada.");
        }
        Conta destino = Conta.findById(dto.contaDestinoId);
        if (destino == null) {
            throw new NotFoundException("Conta de destino não encontrada.");
        }
        if (origem.saldo < dto.valor) {
            throw new BadRequestException("Saldo insuficiente para realizar a transferência.");
        }

        origem.saldo -= dto.valor;
        destino.saldo += dto.valor;

        Transacao transacao = new Transacao();
        transacao.tipo = TipoTransacao.TRANSFERENCIA;
        transacao.valor = dto.valor;
        transacao.contaOrigem = origem;
        transacao.contaDestino = destino;
        transacao.dataHora = LocalDateTime.now();
        transacao.persist();

        return new TransacaoResponseDTO(transacao, origem.saldo);
    }

    private String gerarNumeroConta() {
        long count = Conta.count() + 1;
        return String.format("%04d-%d", count, count % 10);
    }
}
