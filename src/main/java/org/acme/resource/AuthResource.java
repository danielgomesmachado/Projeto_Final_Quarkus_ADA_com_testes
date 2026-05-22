package org.acme.resource;

import io.smallrye.jwt.build.Jwt;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.entity.Cliente;

import java.util.Set;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequest request) {
        Cliente cliente = Cliente.find("email = ?1 and senha = ?2", request.email, request.senha)
                .firstResult();

        if (cliente == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErroDTO("Email ou senha inválidos"))
                    .build();
        }

        String token = Jwt.issuer("quarkus")
                .groups(Set.of(cliente.role))
                .subject(cliente.email)
                .claim("clienteId", cliente.id)
                .expiresIn(3600)
                .sign();

        return Response.ok(new TokenResponse(token)).build();
    }

    public static class LoginRequest {
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        public String email;

        @NotBlank(message = "Senha é obrigatória")
        public String senha;
    }

    public static class TokenResponse {
        public String token;
        public TokenResponse(String token) { this.token = token; }
    }

    public record ErroDTO(String erro) {}
}
