package org.acme.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.acme.dto.TransacaoResponseDTO;
import org.acme.service.TransacaoService;

import java.util.List;

@Path("/transacoes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"GERENTE", "CLIENTE"})
public class TransacaoResource {

    @Inject
    TransacaoService transacaoService;

    @GET
    @Path("/{id}")
    public TransacaoResponseDTO buscarPorId(@PathParam("id") Long id) {
        return transacaoService.buscarPorId(id);
    }

    @GET
    public List<TransacaoResponseDTO> listarPorConta(@QueryParam("contaId") Long contaId) {
        if (contaId == null) {
            throw new BadRequestException("Parâmetro contaId é obrigatório.");
        }
        return transacaoService.listarPorConta(contaId);
    }
}
