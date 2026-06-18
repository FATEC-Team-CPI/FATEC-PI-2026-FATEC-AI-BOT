package org.acme.users.service;

import org.acme.users.dto.CreateUserRequest;
import org.acme.users.dto.CreateUserResponse;
import org.acme.users.dto.LoginResponse;
import org.acme.users.dto.TokenUserResponse;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Service Interface: Contrato da camada de negócio
 * Define os casos de uso disponíveis para o domínio Users
 * Implementação desacoplada: Resource depende desta interface, não da implementação
 */
public interface IUserService {
    
    /**
     * Caso de uso: Criar novo administrador
     * @param request dados do novo admin
     * @return response com dados do admin criado
     * @throws IllegalArgumentException se email já existe
     */
    CreateUserResponse cadastrar(CreateUserRequest request) throws Exception;
    //
    
    /**
     * Caso de uso: Buscar admin por email
     * @param email do admin
     * @return response com dados do admin ou lança exceção
     */
    CreateUserResponse buscarPorEmail(String email) throws Exception;

    /**
     * Caso de uso: Validar token JWT
     * Verifica validade do token e extrai informações
     * @param jwt token JWT validado pelo Quarkus
     * @return response com dados do token e tempo de expiração
     * @throws IllegalArgumentException se token inválido/expirado
     */
    TokenUserResponse validarToken(JsonWebToken jwt) throws Exception;

    /**
     * Caso de uso: Login do usuário
     * Autentica usuário por email e senha, retornando JWT
     * @param unidade unidade FATEC (ex: "Itaquera")
     * @param email email do usuário
     * @param password senha em plain text
     * @return LoginResponse com token JWT e mensagem de sucesso
     * @throws SecurityException se credenciais inválidas ou usuário inativo
     */
    LoginResponse loginProcess(String unidade, String email, String password) throws Exception;
    //comentarios são importantes para documentacao

    /**
     * Lista todos os administradores/editores
     */
    java.util.List<CreateUserResponse> listarAdmins() throws Exception;

}

