package org.acme.aibot.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "UploadDocResponse", description = "Resposta do upload de documento")
public record UploadDocResponse(
    Boolean sucesso,
    String mensagem,
    String key
) {}