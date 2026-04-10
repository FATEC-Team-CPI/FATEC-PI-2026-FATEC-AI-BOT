package org.acme.users.service;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.Optional;

import org.acme.users.dto.CreateUserRequest;
import org.acme.users.dto.CreateUserResponse;
import org.acme.users.dto.TokenUserRequest;
import org.acme.users.dto.TokenUserResponse;
import org.acme.users.model.User;
import org.acme.users.repository.IUserRepository;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jose4j.jwt.consumer.InvalidJwtException;

import io.quarkus.elytron.security.common.BcryptUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service Implementation: Lógica de negócio concreta
 * Orquestra as operações do domínio com o repositório
 * Responsável por validações de negócio e transformações
 * 
 * Implementação de: IUserService
 */
@ApplicationScoped
public class UserService implements IUserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Inject
    IUserRepository repository;

    @Inject
    JsonWebToken jwt;

    
    /**
     * Caso de uso: Criar novo administrador
     */
    @Override
    public CreateUserResponse cadastrar(CreateUserRequest request) throws Exception {
        logger.info("Iniciando criação de admin para: {}", request.email());
        
        // 1. Verificar se já existe
        Optional<User> existente = repository.findByEmail(request.email());
        if (existente.isPresent()) {
            throw new IllegalArgumentException("Email já cadastrado: " + request.email());
        }
        
        // 2. Criar entidade de domínio
        User admin = User.criar(
            request.unidade(),
            request.email(),
            request.name(),
            this.hashPassword(request.password()),
            request.role()
        );
        
        // 3. Persistir (adapter será injetado)
        repository.save(admin);
        logger.info("Administrador criado com sucesso: {}", admin.sk());
        
        // 4. Retornar DTO de resposta
        return toCreateUserResponse(admin);
    }
    
    /**
     * Caso de uso: Buscar admin por email
     */
    @Override
    public CreateUserResponse buscarPorEmail(String email) throws Exception {
        logger.info("Buscando admin por email: {}", email);
        return repository.findByEmail(email)
            .map(this::toCreateUserResponse)
            .orElseThrow(() -> new IllegalArgumentException("Admin não encontrado: " + email));
    }


    @Override
    @RolesAllowed("admin")
    public TokenUserResponse validarToken(TokenUserRequest token) throws Exception{
        logger.info("Validando token: {}", token);

        try {
            Instant tokenResponseHrLimite = Instant.now().plusSeconds(7200);
            return new TokenUserResponse("Token válido", tokenResponseHrLimite);

            // String subject = jwt.getSubject(); // usuário
            // Instant exp = parseInstant(jwt.getExpirationTime());


        } catch (Exception e) {
            throw new Exception(e.getMessage());
            //cria objeto throw a partir da classe exeception e retorna a mensagem de erro
        }        

    }
    
    
    // ========== PRIVATE METHODS ==========
    
    /**
     * Hash a senha usando BCrypt com Quarkus Elytron
     * Cada chamada gera um hash diferente (salt único automático)
     * @param password senha em plain text
     * @return senha hasheada com BCrypt
     */
    private String hashPassword(String password) {
        return BcryptUtil.bcryptHash(password);
    }
    
    /**
     * Verifica se a senha fornecida corresponde ao hash armazenado
     * Use isso em casos de login/autenticação
     * @param senhaPlain senha em plain text (do formulário)
     * @param senhaHasheada hash armazenado no DynamoDB
     * @return true se a senha está correta
     */
    public boolean verificarSenha(String senhaPlain, String senhaHasheada) {
        return BcryptUtil.matches(senhaPlain, senhaHasheada);
    }
    
    private CreateUserResponse toCreateUserResponse(User admin) {
        return new CreateUserResponse(
            admin.sk(),
            admin.name(),
            admin.role(),
            admin.status(),
            admin.createdAt(),
            admin.updatedAt()
        );
    }

    
}
