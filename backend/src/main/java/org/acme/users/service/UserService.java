package org.acme.users.service;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.Optional;

import org.acme.users.dto.CreateUserRequest;
import org.acme.users.dto.CreateUserResponse;
import org.acme.users.dto.LoginResponse;
import org.acme.users.dto.TokenUserResponse;
import org.acme.users.model.User;
import org.acme.users.repository.IUserRepository;
import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.stream.Collectors;


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

    
    private void validateCreateUserRequest(CreateUserRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("Email é obrigatório");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Senha é obrigatória");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Nome é obrigatório");
        }
        if (request.role() == null || request.role().isBlank()) {
            throw new IllegalArgumentException("Role é obrigatória");
        }
        if (!request.role().equals("SUPER_ADMIN") && !request.role().equals("EDITOR")) {
            throw new IllegalArgumentException("Role inválida: " + request.role());
        }
    }

    /**
     * Caso de uso: Criar novo administrador
     */
    @Override
    public CreateUserResponse cadastrar(CreateUserRequest request) throws Exception {
        logger.info("Iniciando criação de admin para: {}", request.email());
        validateCreateUserRequest(request);

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

    /**
     * Caso de uso: Login do usuário
     * Autentica usando chaves de partição e ordenação
     * Valida senha e retorna JWT
     */
    @Override
    public LoginResponse loginProcess(String unidade, String email, String password) throws Exception {
        logger.info("Iniciando login para: {}", email);
        
        // Sanitização e Validação
        String normalizedEmail = email.trim().toLowerCase();
        String normalizedUnit = unidade.trim();
        String pk = normalizedUnit + "#USERS";

        // Validar se existe no DB usando a chave real da unidade + email
        Optional<User> userOpt = repository.findByKeys(pk, normalizedEmail);
        if (userOpt.isEmpty()) {
            logger.warn("Usuário não encontrado para login: {} (pk={})", normalizedEmail, pk);
            throw new SecurityException("Inválido");
        }

        User user = userOpt.get();

        // Validar Match de Senha e Status
        if (!"active".equals(user.status()) || !BcryptUtil.matches(password, user.passwordHash())) {
            logger.warn("Falha na autenticação para: {} - Status: {}", email, user.status());
            throw new SecurityException("Inválido");
        }

        // JWT real com dados do usuário
        String token = Jwt.claims()
            .issuer("fatec-auth")
            .subject(user.sk())
            .claim("name", user.name())
            .claim("email", user.sk())
            .claim("role", user.role())
            .claim("status", user.status())
            .expiresIn(3600)
            .sign();

        logger.info("Login realizado com sucesso para: {}", email);
        return new LoginResponse(token, "Login realizado com sucesso");
    }

    @Override
    public TokenUserResponse validarToken(JsonWebToken jwt) throws Exception {
        logger.info("Validando token JWT para usuário: {}", jwt.getSubject());
        
        try {
            // Token já foi validado pelo Quarkus
            // Se chegou aqui, é válido
            
            String subject = jwt.getSubject();
            
            Instant expirationTime = Instant.ofEpochSecond(jwt.getExpirationTime());
            
            // Validar se token não expirou
            if (Instant.now().isAfter(expirationTime)) {
                throw new IllegalArgumentException("Token expirado");
            }
            
            logger.info("Token válido. Usuário: {}, Expira em: {}", subject, expirationTime);
            
            return new TokenUserResponse(
                "Token válido para: " + subject,
                expirationTime
            );
            
        } catch (IllegalArgumentException e) {
            logger.warn("Token inválido: {}", e.getMessage());
            throw e;
            
        } catch (Exception e) {
            logger.error("Erro ao validar token", e);
            throw new IllegalArgumentException("Erro ao processar token: " + e.getMessage());
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

    @Override
    public List<CreateUserResponse> listarAdmins() throws Exception {
        var users = repository.findByPartitionKey("FatecItaquera#USERS");
        return users.stream().map(this::toCreateUserResponse).collect(Collectors.toList());
    }

    
}
