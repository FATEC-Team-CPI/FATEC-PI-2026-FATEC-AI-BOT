package org.acme.users.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.acme.users.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Adapter: Implementação de persistência com DynamoDB
 * Converte entre objetos de domínio e comandos DynamoDB
 * Implementa a interface IUserRepository (Port)
 */
@ApplicationScoped
public class DynamoDBUserRepository implements IUserRepository {
    private static final Logger logger = LoggerFactory.getLogger(DynamoDBUserRepository.class);
    private static final String TABLE_NAME = "fatec-ai-bot-core";
    
    @Inject
    DynamoDbClient dynamodb;
    
    @Override
    public void save(User admin) throws Exception {
        logger.debug("Salvando admin no DynamoDB: {}", admin.sk());
        
        // construir mapa de atributos para o item DynamoDB
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("pk", AttributeValue.builder().s(admin.pk()).build());
        item.put("sk", AttributeValue.builder().s(admin.sk()).build());
        item.put("entityType", AttributeValue.builder().s("ADMIN").build());
        item.put("name", AttributeValue.builder().s(admin.name()).build());
        item.put("password_hash", AttributeValue.builder().s(admin.passwordHash()).build());
        item.put("role", AttributeValue.builder().s(admin.role()).build());
        item.put("status", AttributeValue.builder().s(admin.status()).build());
        item.put("created_at", AttributeValue.builder().n(String.valueOf(admin.createdAt().getEpochSecond())).build());
        item.put("updated_at", AttributeValue.builder().n(String.valueOf(admin.updatedAt().getEpochSecond())).build());
        
        // GSI1 para busca por email
        item.put("gsi1pk", AttributeValue.builder().s("AUTH#EMAIL").build());
        item.put("gsi1sk", AttributeValue.builder().s(admin.sk()).build());
        

        // construir comando PutItem para salvar a entidade no DynamoDB
        PutItemRequest request = PutItemRequest.builder()
            .tableName(TABLE_NAME)
            .item(item)
            .build();
        

        // salva a entidade no DynamoDB

        
        dynamodb.putItem(request);
        logger.info("Admin salvo com sucesso: {}", admin.sk());
    }

    @Override
    public Optional<User> findByEmail(String email) throws Exception {
        logger.debug("Buscando usuário por email: {}", email);
        
        // PK: FatecItaquera#USERS (agrupa usuários da unidade)
        // SK: email (identificador globalmente único)
        String pk = "FatecItaquera#USERS";
        
        GetItemRequest request = GetItemRequest.builder()
            .tableName(TABLE_NAME)
            .key(Map.of(
                "pk", AttributeValue.builder().s(pk).build(),
                "sk", AttributeValue.builder().s(email).build()
            ))
            .build();
        
        GetItemResponse response = dynamodb.getItem(request);
        
        if (!response.hasItem()) {
            logger.debug("Usuário não encontrado para email: {}", email);
            return Optional.empty();
        }
        
        User user = mapToUser(response.item());
        logger.info("Usuário encontrado para email: {}", email);
        return Optional.of(user);
    }
    
    // ========== PRIVATE METHODS ==========
    
    private User mapToUser(Map<String, AttributeValue> item) {
        String pk = item.get("pk").s();
        String sk = item.get("sk").s();
        String name = item.get("name").s();
        String passwordHash = item.get("password_hash").s();
        String role = item.get("role").s();
        String status = item.get("status").s();
        long createdAtEpoch = Long.parseLong(item.get("created_at").n());
        long updatedAtEpoch = Long.parseLong(item.get("updated_at").n());
        
        return User.fromDynamoDB(pk, sk, name, passwordHash, role, status, createdAtEpoch, updatedAtEpoch);
    }
}
