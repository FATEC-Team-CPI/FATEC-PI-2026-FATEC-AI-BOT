package org.acme.aibot.service;

import org.acme.users.repository.IUserRepository;
import org.acme.aibot.dto.UploadDocRequest;
import org.acme.aibot.dto.UploadDocResponse;
import org.acme.aibot.model.Documento;
import org.acme.aibot.repository.DynamoDBAIBotRepository;
import org.acme.aibot.service.IAIBotService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.LoggerFactory;

import io.quarkus.runtime.annotations.ConfigDocDefault;

import org.apache.tika.Tika;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import org.jboss.resteasy.reactive.multipart.FileUpload;



/**
 * Service Implementation: Lógica de negócio concreta
 * Orquestra as operações do domínio com o repositório
 * Responsável por validações de negócio e transformações
 * 
 * Implementação de: IIABotService
 */
@ApplicationScoped
public class AIBotService implements IAIBotService {
    // private static final Logger logger = LoggerFactory.getLogger(AIBotService.class);
    
    // @Inject
    // IUserRepository repository;

    @Inject
    S3Client s3Client;

    @Inject
    DynamoDbEnhancedClient enhancedClient;    

    @ConfigProperty(name = "bucket.name")
    String bucketName;

    @ConfigProperty(name = "dynamodb.table.conteudos") //tabela do dynamon
    String tableName;

    @Inject 
    DynamoDBAIBotRepository repository;


    @Override
    public boolean validarTipoDocumento(UploadDocRequest documentoUpload) throws WebApplicationException {
        // logger.info("Iniciando criação de admin para: {}", request.email());

        List<String> TIPOS_PERMITIDOS = List.of(
        "application/pdf",
        "application/docx",
        "application/xlsx",
        "apllication/pptx",
        "application/html",
        "application/xhtml",
        "application/csv",
        "application/markdown",
        "image/png",
        "image/jpeg",
        "image/tiff",
        "image/bmp",
        "image/webp"
        );
        //arquivos suportados pelo docling

        final Tika TIKA = new Tika();

        try {
            File file = documentoUpload.document.uploadedFile().toFile();
            //documentoupload é o corpo inteiro da requsição, e document é o campo dentro dele
            String tipoDocumento = TIKA.detect(file); 

            if (!TIPOS_PERMITIDOS.contains(tipoDocumento)) {
                throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                        .entity("Tipo de arquivo inválido: " + tipoDocumento)
                        .build()
                );
            }
            return true;

        } catch (IOException e) {
            throw new WebApplicationException(
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro ao processar documento: " + e.getMessage())
                    .build()
            );
        }

    }

    @Override
    public UploadDocResponse uploadDocumento(UploadDocRequest documentoUpload) throws WebApplicationException {

        /*Envia documento bruto para o LocalStack e os metadados para o DynamoDB */

        if (!validarTipoDocumento(documentoUpload)) {
            throw new WebApplicationException(
                Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                    .entity("Tipo de documento inválido")
                    .build()
            );
        }

        try {
            FileUpload fileUpload = documentoUpload.document; 
            byte[] conteudo = Files.readAllBytes(fileUpload.filePath());
            String fileName = fileUpload.fileName();
            String key = "fatec-itaquera/conteudos/" + fileName+ "-" + Instant.now().toEpochMilli(); 

            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(fileUpload.contentType())
                    .build(),
                RequestBody.fromBytes(conteudo)
            );

            try{
                uploadDetalhesDocumentoNoDB(fileName, key);
            }catch (Exception e) {
                // Se falhar ao salvar no DB, tenta deletar o arquivo do S3 para evitar inconsistência
                s3Client.deleteObject(builder -> builder.bucket(bucketName).key(key).build());
                throw e; // Re-throw a exceção para ser tratada no nível superior
            }
            
            return new UploadDocResponse(true, "Documento enviado salvo com sucesso", key);

        } catch (IOException e) {
            throw new WebApplicationException(
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro ao processar documento: " + e.getMessage())
                    .build()
            );
        }
    }

    @Override
    public UploadDocResponse uploadDetalhesDocumentoNoDB(String fileName, String key) throws WebApplicationException {
        try {
            repository.uploadMDnoDynamonDB(Documento.criar(fileName, key));
            //documento.criar -> cria o objeto documento
            //repository.uploadMDnoDynamonDB -> salva o objeto que criei no db

            return new UploadDocResponse(true, "Metadados do documento enviado para DB com sucesso", key);

        } catch (WebApplicationException e) {
            throw new WebApplicationException(
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro ao processar documento: " + e.getMessage())
                    .build()
            );
        }
    }
}