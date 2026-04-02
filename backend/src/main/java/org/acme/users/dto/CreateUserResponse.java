package org.acme.users.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.time.Instant;

@Schema(name = "Response", description = "Resposta com dados do administrador")
public record CreateUserResponse(
    @Schema(description = "ID/Chave da unidade", examples = "FatecItaquera#users")
    String pk,
    
    @Schema(description = "Email do administrador", examples = "admin@fatec.com")
    String sk,
    
    @Schema(description = "Nome do administrador", examples = "João Silva")
    String name,
    
    @Schema(description = "Papel do administrador", examples = "SUPER_ADMIN")
    String role,
    
    @Schema(description = "Status do administrador", examples = "active")
    String status,
    
    @Schema(description = "Data de criação em ISO-8601", examples = "2026-04-02T10:00:00Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant createdAt,
    
    @Schema(description = "Data de atualização em ISO-8601", examples = "2026-04-02T10:00:00Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant updatedAt
) {}
