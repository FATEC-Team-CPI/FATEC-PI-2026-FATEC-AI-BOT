package org.acme.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @NotBlank(message="Unidade é obrigatória")
    public String unidade;

    @NotBlank(message="Email é obrigatório")
    @Email(message="Email inválido")
    public String email;

    @NotBlank(message="Senha é obrigatória")
    public String password;
}