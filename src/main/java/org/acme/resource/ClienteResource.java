package org.acme.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.dto.ClienteRequestDTO;
import org.acme.dto.ClienteResponseDTO;
import org.acme.service.ClienteService;

import java.util.List;

@Path("/clientes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("GERENTE")
public class ClienteResource {

    @Inject
    ClienteService clienteService;

    @GET
    public List<ClienteResponseDTO> listar() {
        return clienteService.listarTodos();
    }

    @GET
    @Path("/{id}")
    public ClienteResponseDTO buscarPorId(@PathParam("id") Long id) {
        return clienteService.buscarPorId(id);
    }

    @POST
    public Response criar(@Valid ClienteRequestDTO dto) {
        ClienteResponseDTO criado = clienteService.criar(dto);
        return Response.status(Response.Status.CREATED).entity(criado).build();
    }

    @PUT
    @Path("/{id}")
    public ClienteResponseDTO atualizar(@PathParam("id") Long id, ClienteRequestDTO dto) {
        return clienteService.atualizar(id, dto);
    }
}
