package org.acme;


import java.time.Instant;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

// import org.acme.dto.GreetingRequest;
// import org.acme.dto.GreetingResponse;

@Path("/hello")
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        

        return "Helloooooooow from Quarkus REST";
    }

    // @POST
    // @Consumes(MediaType.APPLICATION_JSON)
    // @Produces(MediaType.APPLICATION_JSON)
    // public GreetingResponse greeting(GreetingRequest request) {
    //     String message = request.name() != null 
    //         ? "Hello, " + request.name() + "!" 
    //         : "Hello, World!";
    //     return new GreetingResponse(message, Instant.now());
    // }


}
