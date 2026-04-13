package org.acme.users.repository;

import java.util.Optional;

import org.acme.users.model.User;

/**
 * Port (Interface): Contrato de persistência
 * Define as operações que o repositório deve implementar
 * Desacopla a lógica de negócio da implementação de dados
 */
public interface IUserRepository {
    
    /**
     * Salva um novo administrador
     */
    void save(User admin) throws Exception;
    // public void save(User admin) throws Exception
    
    /**
     * Busca um administrador por email
     */
    Optional<User> findByEmail(String email) throws Exception;

    /**
     * Busca um usuário usando chaves de partição e ordenação
     * Util para desenvolvimento/teste com mock
     */
    Optional<User> findByKeys(String pk, String sk) throws Exception;

    
}
