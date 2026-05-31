package org.acme.users;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.acme.users.dto.CreateUserRequest;
import org.acme.users.dto.CreateUserResponse;
import org.acme.users.dto.LoginRequest;
import org.acme.users.dto.LoginResponse;
import org.acme.users.dto.TokenUserResponse;
import org.acme.users.service.IUserService;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

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
    /**
     * POST /admin/login
     * Realiza login de usuário e retorna JWT
     */
    @POST
    @Path("/login")
    @Operation(summary = "Login de usuário", description = "Autentica usuário com email e senha, retornando JWT")
    @APIResponse(responseCode = "200", description = "Login realizado com sucesso",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class)))
    @APIResponse(responseCode = "401", description = "Credenciais inválidas")
    public Response login(LoginRequest request) {
        try {
            logger.info("Requisição de login para: {}", request.email);
            var loginResponse = service.loginProcess(request.unidade, request.email, request.password);
            return Response.ok(loginResponse).build();
        } catch (SecurityException e) {
            logger.warn("Tentativa de login falhou: {}", request.email);
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity(Map.of("error", "Usuário ou senha inválidos"))
                           .build();
        } catch (Exception e) {
            logger.error("Erro ao fazer login", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Erro ao autenticar usuário"))
                .build();
        }
    }

    // criar metodo de logout
    /**
     * POST /admin/token
     * Valida um token JWT recebido no header Authorization
     * Delega validação para service (regra de negócio)
     */
    @POST
    @Path("/token")
    @RolesAllowed("**") // Força validação JWT automática - rejeita requisições sem token válido
    @Operation(summary = "Validar token JWT", description = "Valida um token JWT e retorna informações sobre validade")
    @APIResponse(responseCode = "200", description = "Token válido",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenUserResponse.class)))
    @APIResponse(responseCode = "401", description = "Token inválido ou expirado")
    public Response validarToken(@Context JsonWebToken jwt) {
        try {
            logger.info("Requisição de validação de token JWT");
            
            // Delega a lógica de validação para o service
            TokenUserResponse response = service.validarToken(jwt);
            return Response.ok(response).build();

        } catch (IllegalArgumentException e) {
            logger.warn("Token inválido: {}", e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Map.of("error", "Token inválido"))
                .build();

        } catch (Exception e) {
            logger.error("Erro ao validar token", e);
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Map.of("error", "Token inválido ou expirado"))
                .build();
        }
    }

    /**
     * GET /admin/list
     * Lista todos os administradores/editores
     */
    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Listar administradores", description = "Retorna todos os administradores/editores cadastrados")
    public Response listarAdmins() {
        try {
            List<CreateUserResponse> admins = service.listarAdmins();
            return Response.ok(admins).build();
        } catch (Exception e) {
            logger.error("Erro ao listar administradores", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(java.util.Map.of("error", "Erro ao listar administradores: " + e.getMessage()))
                .build();
        }
    }

}
