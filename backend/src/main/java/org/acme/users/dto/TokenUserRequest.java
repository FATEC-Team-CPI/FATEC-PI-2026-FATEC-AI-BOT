package org.acme.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "Request", description = "Requisição para validar token JWT")
public record TokenUserRequest (

    @NotBlank(message = "Token não pode estar vazio")
    @Pattern(regexp = "^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$", message = "Formato de token inválido")

    String token
) {}
