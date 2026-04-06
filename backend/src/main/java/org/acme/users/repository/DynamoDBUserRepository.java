package org.acme.users.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.annotation.PostConstruct;

import org.acme.users.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Optional;


/**
 * Adapter: Implementação de persistência com DynamoDB (Enhanced Client)
 * Usa anotações @DynamoDbBean para mapeamento automático de entidades
 * Implementa a interface IUserRepository (Port)
 */
@ApplicationScoped
public class DynamoDBUserRepository implements IUserRepository {
    private static final Logger logger = LoggerFactory.getLogger(DynamoDBUserRepository.class);
    private static final String TABLE_NAME = "fatec-ai-bot-core";
    
    @Inject
    DynamoDbEnhancedClient dynamodb;
    
    private DynamoDbTable<User> table;
    
    @PostConstruct
    void init() {
        // Inicializa a tabela com o schema inferido das anotações
        this.table = dynamodb.table(TABLE_NAME, TableSchema.fromClass(User.class));
        logger.info("DynamoDB table '{}' initialized", TABLE_NAME);
    }
    
    @Override
    public void save(User user) throws Exception {
        logger.debug("Salvando usuário no DynamoDB: {}", user.sk());
        
        // Enhanced Client cuida automaticamente do mapeamento!
        table.putItem(user);
        
        logger.info("Usuário salvo com sucesso: {}", user.sk());
    }

    @Override
    public Optional<User> findByEmail(String email) throws Exception {
        logger.debug("Buscando usuário por email: {}", email);
        
        // PK: FatecItaquera#USERS (agrupa usuários da unidade)
        // SK: email (identificador globalmente único)
        String pk = "FatecItaquera#USERS";
        
        // Enhanced Client - sem AttributeValue manual!
        User user = table.getItem(r -> r
            .key(k -> k
                .partitionValue(pk)
                .sortValue(email)
            )
        );
        
        if (user == null) {
            logger.debug("Usuário não encontrado para email: {}", email);
            return Optional.empty();
        }
        
        logger.info("Usuário encontrado para email: {}", email);
        return Optional.of(user);
    }
}
