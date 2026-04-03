package org.acme.users.service;

import org.acme.users.dto.CreateUserRequest;
import org.acme.users.dto.CreateUserResponse;

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

    // caso de uso: login
    

}
