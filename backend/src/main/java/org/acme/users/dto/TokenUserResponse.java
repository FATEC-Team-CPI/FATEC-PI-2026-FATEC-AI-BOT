package org.acme.users.dto;

import java.time.Instant;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonFormat;

@Schema(name="Response", description = "Resposta com status de validação token e tempo de experiração")
public record TokenUserResponse (
    @Schema(description = "Status do Token")
    String tokenResponse,

    @Schema(description = "Tempo até expiração do token")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant tempoLimite

){}
