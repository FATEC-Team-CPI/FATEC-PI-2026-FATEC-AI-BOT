package org.acme.aibot.repository;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.acme.aibot.model.Documento;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;



/**
 * Adapter: Implementação de persistência com DynamoDB (Enhanced Client)
 * Usa anotações @DynamoDbBean para mapeamento automático de entidades
 * Implementa a interface IUserRepository (Port)
 */
@ApplicationScoped
public class DynamoDBAIBotRepository implements IAIBotRepository {
    private static final Logger logger = LoggerFactory.getLogger(DynamoDBAIBotRepository.class.getName());
    private static final String TABLE_NAME = "fatec-ai-bot-core";

    @Inject
    DynamoDbEnhancedClient dynamodb;
    
    private DynamoDbTable<Documento> table;

        
    @PostConstruct
    void init() {
        // Inicializa a tabela com o schema inferido das anotações
        this.table = dynamodb.table(TABLE_NAME, TableSchema.fromImmutableClass(Documento.class));
        logger.info("DynamoDB table '{}' initialized", TABLE_NAME);
    }
    

    @Override
    public void uploadMDnoDynamonDB(Documento documento) throws WebApplicationException {
        logger.info("Salvando metadados do documento no DynamoDB: {}", String.valueOf(documento.sk()));

        try {
            table.putItem(documento);
        } catch (WebApplicationException e) {
            logger.error("Erro ao salvar metadados do documento: {}", e.getMessage());
            throw new WebApplicationException(
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro ao processar documento: " + e.getMessage())
                    .build()
            );        }

        logger.info("Metadados do documento salvo com sucesso: {}", documento.sk());
    }
    
    @Override
    public List<Documento> listarDocumentos() throws WebApplicationException {
        try {
            List<Documento> results = new ArrayList<>();
            String pk = "FatecItaquera#Conteudos";
            this.table.query(r -> r.queryConditional(
                QueryConditional.keyEqualTo(k -> k.partitionValue(pk))
            )).items().forEach(results::add);
            return results;
        } catch (Exception e) {
            logger.error("Erro ao listar documentos: {}", e.getMessage());
            throw new WebApplicationException(
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro ao listar documentos: " + e.getMessage())
                    .build()
            );
        }
    }
}
