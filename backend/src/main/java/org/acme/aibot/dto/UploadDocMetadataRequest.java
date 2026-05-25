package org.acme.aibot.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "UploadDocMetadataRequest", description = "Requisição para salvar metadados do documento no DynamoDB")
public record UploadDocMetadataRequest(
    String key,         // chave gerada no S3
    String fileName,    // nome original do arquivo
    String contentType, // tipo do arquivo (application/pdf, image/png...)
    Long size           // tamanho em bytes
) {}