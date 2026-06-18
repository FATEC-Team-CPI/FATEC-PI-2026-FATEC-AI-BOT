package org.acme.aibot.model;

import java.time.Instant;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbImmutable(builder = Documento.Builder.class)
public record Documento(
    //aqui ele define quais campos serão usados pelo DynamoDB para armazenar os dados do documento, 
    //e como eles serão organizados
    @DynamoDbPartitionKey
    String pk,

    @DynamoDbSortKey
    String sk,

    @DynamoDbAttribute("entityType")
    String entityType,

    @DynamoDbAttribute("s3Key")
    String s3Key,   

    @DynamoDbAttribute("status")
    String status,

    @DynamoDbAttribute("gsi2pk")
    String gsi2pk,  

    @DynamoDbAttribute("gsi2sk")
    String gsi2sk
) {

    // Factory method para criar novo documento
    public static Documento criar(String fileName, String key) {
        String timeNow = Instant.now().toString();

        return new Documento(
            "FatecItaquera#Conteudos",                 // PK: caminho fixo para conteúdos
            fileName,                                  // SK: nome do arquivo (único)
            "CONTENT",
            key,
            "ACTIVE",
            "UNIT#FatecItaquera#CONTENT",              // GSI2PK: agrupa por unidade e tipo
            "STATUS#ACTIVE#TS#" + timeNow + "#" + fileName // GSI2SK: ordena por status e timestamp
        );
    }

    //esse metodo é usado pelo SDK para reconstruir o objeto
    //toda vez que chamo por ele, ele pega os dados do DynamoDB
    //e transforma em um objeto Documento
    public static class Builder {
        private String pk;
        private String sk;
        private String entityType;
        private String s3Key;
        private String status;
        private String gsi2pk;
        private String gsi2sk;

        public Builder pk(String pk) {
            this.pk = pk;
            return this;
        }

        public Builder sk(String sk) {
            this.sk = sk;
            return this;
        }

        public Builder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }

        public Builder s3Key(String s3Key) {
            this.s3Key = s3Key;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder gsi2pk(String gsi2pk) {
            this.gsi2pk = gsi2pk;
            return this;
        }

        public Builder gsi2sk(String gsi2sk) {
            this.gsi2sk = gsi2sk;
            return this;
        }

        public Documento build() {
            return new Documento(pk, sk, entityType, s3Key, status, gsi2pk, gsi2sk);
        }
    }

}





