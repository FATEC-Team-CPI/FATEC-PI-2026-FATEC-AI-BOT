package org.acme.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.model.Admin;
import org.acme.model.LoginResponse;
import org.acme.repository.IAdminRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;

@ApplicationScoped
public class AuthService implements IAuthService {

    @Inject
    IAdminRepository repository;

    public LoginResponse loginProcess(String unidade, String email, String pass) {
        // Sanitização e Validação
        String sk = email.trim().toLowerCase();
        String pk = "Fatec" + unidade.trim() + "#users";

        // Validar se existe no DB
        Admin admin = repository.findByKeys(pk, sk);

        // Validar Match de Senha e Status
        if (admin == null || !"active".equals(admin.getStatus()) || 
            !BcryptUtil.matches(pass, admin.getPasswordHash())) {
            throw new SecurityException("Invalido");
        }

        // JWT real (mock de dados de usuário ainda + DB simulado)
        String token = Jwt.claims()
            .issuer("fatec-auth")
            .subject(admin.getSk())
            .claim("pk", admin.getPk())
            .claim("status", admin.getStatus())
            .expiresIn(3600)
            .sign();

        return new LoginResponse(token, "Login realizado com sucesso");
    }
}