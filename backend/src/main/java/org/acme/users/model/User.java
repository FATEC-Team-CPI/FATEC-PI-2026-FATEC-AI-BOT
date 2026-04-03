package org.acme.users.model;

import java.time.Instant;

/**
 * Entidade de domínio: User (Record - Imutável)
 * Representa um usuário da plataforma FATEC AI BOT
 */
public record User(
    String pk,              // FatecItaquera#USERS
    String sk,              // email
    String name,
    String passwordHash,
    String role,            // SUPER_ADMIN, EDITOR
    String status,          // active, nonactive
    Instant createdAt,
    Instant updatedAt
) {


    
    // Factory method para criar novo user
    public static User criar(String unidade, String email, String name, String passwordHash, String role) {
        return new User(
            unidade + "#USERS",                 // PK: agrupa usuários da unidade
            email,                              // SK: email único globalmente
            name,
            passwordHash,
            role,
            "active",
            Instant.now(),
            Instant.now()
        );
    }

    // Factory method para reconstruir do DynamoDB
    public static User fromDynamoDB(String pk, String sk, String name, String passwordHash, 
                                    String role, String status, long createdAtEpoch, long updatedAtEpoch) {
        return new User(
            pk,
            sk,
            name,
            passwordHash,
            role,
            status,
            Instant.ofEpochSecond(createdAtEpoch),
            Instant.ofEpochSecond(updatedAtEpoch)
        );
    }

    // Método para verificar se está ativo
    public boolean isActive() {
        return "active".equals(status);
    }

    // Método para desativar (retorna novo User)
    public User deactivate() {
        return new User(pk, sk, name, passwordHash, role, "nonactive", createdAt, Instant.now());
    }
}
