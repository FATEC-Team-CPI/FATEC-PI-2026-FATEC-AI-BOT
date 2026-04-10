package org.acme.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.service.IAuthService;
import java.util.Map;
import org.acme.model.LoginRequest;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    IAuthService authService;

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        try {
            // Captura do Front
            var loginResponse = authService.loginProcess(request.unidade, request.email, request.password);
            return Response.ok(loginResponse).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity(Map.of("error", "Usuário ou senha inválidos"))
                           .build();
        }
    }
}