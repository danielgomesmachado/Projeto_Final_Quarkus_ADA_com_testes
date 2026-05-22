package org.acme.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.acme.dto.TransacaoResponseDTO;
import org.acme.entity.Transacao;

import java.util.List;

@ApplicationScoped
public class TransacaoService {

    public TransacaoResponseDTO buscarPorId(Long id) {
        Transacao transacao = Transacao.findById(id);
        if (transacao == null) {
            throw new NotFoundException("Transação não encontrada com id: " + id);
        }
        return new TransacaoResponseDTO(transacao, null);
    }

    public List<TransacaoResponseDTO> listarPorConta(Long contaId) {
        return Transacao.findByContaId(contaId)
                .stream()
                .map(t -> new TransacaoResponseDTO(t, null))
                .toList();
    }
}
