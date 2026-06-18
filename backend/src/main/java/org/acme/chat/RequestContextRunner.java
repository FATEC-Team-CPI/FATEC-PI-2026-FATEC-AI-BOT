package org.acme.chat;

import java.util.concurrent.Callable;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;

@ApplicationScoped
public class RequestContextRunner {

    @ActivateRequestContext
    public <T> T run(Callable<T> action) throws Exception {
        return action.call();
    }
}
