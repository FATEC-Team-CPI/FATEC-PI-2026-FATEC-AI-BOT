package org.acme.users.dto;

public class LoginResponse {
    public String token;
    public String message;

    public LoginResponse() {}

    public LoginResponse(String token, String message) {
        this.token = token;
        this.message = message;
    }
}