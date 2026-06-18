package org.acme.users.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.annotation.PostConstruct;

import org.acme.users.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import io.quarkus.elytron.security.common.BcryptUtil;

import java.time.Instant;
import java.util.Optional;
import java.util.List;



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

    @Override
    public Optional<User> findByKeys(String pk, String sk) throws Exception {
        logger.debug("Buscando usuário por PK: {} e SK: {}", pk, sk);
        
        // Mock para desenvolvimento/teste sem DB
        if ("FatecItaquera#USERS".equals(pk) && "user@fatec.sp.gov.br".equals(sk)) {
            User mock = new User(
                pk,
                sk,
                "Usuário Teste",
                BcryptUtil.bcryptHash("senha123"),
                "EDITOR",
                "active",
                Instant.now(),
                Instant.now()
            );
            logger.info("Usuário mock retornado para teste: {}", sk);
            return Optional.of(mock);
        }

        // Código real para DynamoDB com Enhanced Client (usar quando DB estiver pronto)
        try {
            User user = table.getItem(r -> r
                .key(k -> k
                    .partitionValue(pk)
                    .sortValue(sk)
                )
            );
            
            if (user == null) {
                logger.debug("Usuário não encontrado para PK: {} SK: {}", pk, sk);
                return Optional.empty();
            }
            
            logger.info("Usuário encontrado: {}", sk);
            return Optional.of(user);
        } catch (Exception e) {
            logger.error("Erro ao buscar usuário no DynamoDB", e);
            // DB não configurado ou erro
        }
        
        return Optional.empty();
    }

    @Override
    public List<User> findByPartitionKey(String pk) throws Exception {
        try {
            List<User> results = new java.util.ArrayList<>();
            this.table.query(r -> r.queryConditional(
                QueryConditional.keyEqualTo(k -> k.partitionValue(pk))
            )).items().forEach(results::add);
            return results;
        } catch (Exception e) {
            logger.error("Erro ao listar usuários por PK: {}", e.getMessage());
            throw e;
        }
    }
}
