package org.acme.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.dto.*;
import org.acme.service.ContaService;

@Path("/contas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContaResource {

    @Inject
    ContaService contaService;

    @POST
    @RolesAllowed("GERENTE")
    public Response criar(@Valid ContaRequestDTO dto) {
        ContaResponseDTO criado = contaService.criar(dto);
        return Response.status(Response.Status.CREATED).entity(criado).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"GERENTE", "CLIENTE"})
    public ContaResponseDTO buscarPorId(@PathParam("id") Long id) {
        return contaService.buscarPorId(id);
    }

    @POST
    @Path("/{id}/deposito")
    @RolesAllowed({"GERENTE", "CLIENTE"})
    public TransacaoResponseDTO depositar(@PathParam("id") Long id, @Valid OperacaoDTO dto) {
        return contaService.depositar(id, dto);
    }

    @POST
    @Path("/{id}/saque")
    @RolesAllowed({"GERENTE", "CLIENTE"})
    public TransacaoResponseDTO sacar(@PathParam("id") Long id, @Valid OperacaoDTO dto) {
        return contaService.sacar(id, dto);
    }

    @POST
    @Path("/{id}/transferencia")
    @RolesAllowed({"GERENTE", "CLIENTE"})
    public TransacaoResponseDTO transferir(@PathParam("id") Long id, @Valid TransferenciaDTO dto) {
        return contaService.transferir(id, dto);
    }
}
