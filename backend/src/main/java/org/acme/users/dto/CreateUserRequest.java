package org.acme.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CreateRequest", description = "Requisição para criar novo administrador")
public record CreateUserRequest(
    @Schema(description = "Nome da unidade FATEC", examples = "FatecItaquera")
    @NotBlank(message = "Unidade não pode estar vazia")


    String unidade,
    
    @Schema(description = "Email do administrador", examples = "admin@fatec.com")
    @NotBlank(message = "Email não pode estar vazio")
    @Email(message = "Email deve ser válido")


    String email,
    
    @Schema(description = "Nome completo do administrador", examples = "João Silva")
    @NotBlank(message = "Nome não pode estar vazio")
    
    String name,
    
    @Schema(description = "Senha (será hashada)", examples = "Senha@123")
    @NotBlank(message = "Senha não pode estar vazia")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%^&+=]).{8,}$", 
             message = "Senha deve ter pelo menos 8 caracteres, uma maiúscula, um número e um caractere especial")
    String password,
    
    @Schema(description = "Papel do administrador", examples = "SUPER_ADMIN")
    @NotBlank(message = "Role não pode estar vazio")
    @Pattern(regexp = "^(SUPER_ADMIN|EDITOR)$", message = "Role deve ser SUPER_ADMIN ou EDITOR")
    String role
) {}
