package org.acme.service;

import org.acme.model.LoginResponse;

public interface IAuthService {
    LoginResponse loginProcess(String unidade, String email, String password);
}