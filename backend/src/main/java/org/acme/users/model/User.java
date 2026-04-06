package org.acme.users.model;

import java.time.Instant;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;

/**
 * Entidade de domínio: User (Record - Imutável)
 * Anotações DynamoDB Enhanced Client para mapeamento automático
 * Representa um usuário da plataforma FATEC AI BOT
 */
@DynamoDbImmutable(builder = User.Builder.class)
public record User(
    @DynamoDbPartitionKey
    String pk,              // FatecItaquera#USERS
    
    @DynamoDbSortKey
    String sk,              // email
    
    @DynamoDbAttribute("name")
    String name,
    
    @DynamoDbAttribute("password_hash")
    String passwordHash,
    
    @DynamoDbAttribute("role")
    String role,            // SUPER_ADMIN, EDITOR
    
    @DynamoDbAttribute("status")
    String status,          // active, nonactive
    
    @DynamoDbAttribute("created_at")
    Instant createdAt,
    
    @DynamoDbAttribute("updated_at")
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
    @DynamoDbIgnore
    public boolean isActive() {
        return "active".equals(status);
    }

    // Método para desativar (retorna novo User)
    @DynamoDbIgnore
    public User deactivate() {
        return new User(pk, sk, name, passwordHash, role, "nonactive", createdAt, Instant.now());
    }
    
    // Builder para DynamoDbImmutable (necessário para Records com @DynamoDbImmutable)
    public static class Builder {
        private String pk;
        private String sk;
        private String name;
        private String passwordHash;
        private String role;
        private String status;
        private Instant createdAt;
        private Instant updatedAt;
        
        public Builder pk(String pk) { this.pk = pk; return this; }
        public Builder sk(String sk) { this.sk = sk; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public Builder role(String role) { this.role = role; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        
        public User build() {
            return new User(pk, sk, name, passwordHash, role, status, createdAt, updatedAt);
        }
    }
}
