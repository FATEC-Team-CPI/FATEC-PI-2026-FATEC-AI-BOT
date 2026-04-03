package org.acme.users;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.acme.users.dto.CreateUserRequest;
import org.acme.users.dto.CreateUserResponse;
import org.acme.users.service.IUserService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Controller/Resource: HTTP Endpoints
 * Orquestra as requisições HTTP com o Service
 * Responsável por serialização/desserialização e respostas HTTP
 */
@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Admin", description = "Gerenciamento de administradores")
public class UserResource {
    private static final Logger logger = LoggerFactory.getLogger(UserResource.class);
    
    @Inject
    IUserService service;

    /**
     * POST /admin
     * Cria um novo administrador
     */
    @POST
    @Path("/create")
    @Operation(summary = "Criar novo administrador", description = "Cria um novo administrador da plataforma")
    @APIResponse(responseCode = "201", description = "Admin criado com sucesso",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreateUserResponse.class)))
    @APIResponse(responseCode = "400", description = "Dados inválidos")
    @APIResponse(responseCode = "409", description = "Email já cadastrado")

    public Response criar(CreateUserRequest request) {
        try {
            logger.info("Requisição para criar admin: {}", request.email());


            CreateUserResponse response = service.cadastrar(request);    


            return Response.status(Response.Status.CREATED).entity(response).build();
        




        } catch (IllegalArgumentException e) {
            logger.warn("Erro ao criar admin: {}", e.getMessage());
            return Response.status(Response.Status.CONFLICT)
                .entity(Map.of("error", e.getMessage()))
                .build();
        } catch (Exception e) {
            logger.error("Erro inesperado ao criar admin", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Erro ao criar administrador"))
                .build();
        }
    }
    
    /**
     * GET /admin/{email}
     * Busca um administrador por email
     */
    @GET
    @Path("{email}")
    @Operation(summary = "Buscar admin por email", description = "Retorna os dados de um admin específico")
    @APIResponse(responseCode = "200", description = "Admin encontrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreateUserResponse.class)))
    @APIResponse(responseCode = "404", description = "Admin não encontrado")
    public Response buscarPorEmail(@PathParam("email") String email) {
        try {
            logger.info("Requisição para buscar admin: {}", email);
            // Delega lógica para Service - Resource apenas trata HTTP
            CreateUserResponse admin = service.buscarPorEmail(email);
            return Response.ok(admin).build();
        } catch (IllegalArgumentException e) {
            logger.warn("Admin não encontrado: {}", email);
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", e.getMessage()))
                .build();
        } catch (Exception e) {
            logger.error("Erro ao buscar admin", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Erro ao buscar administrador"))
                .build();
        }
    }


    // criar metodo de login

    // criar metodo de logout

    // criar metodo de autenticação de token
}
