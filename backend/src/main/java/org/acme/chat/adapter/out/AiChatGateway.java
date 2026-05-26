package org.acme.chat.adapter.out;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.ai.FatecAgent;
import org.acme.chat.core.port.ChatGateway;

@ApplicationScoped
public class AiChatGateway implements ChatGateway {

    @Inject
    FatecAgent fatecAgent;

    @Override
    public String chat(String sessionId, String message) {
        return fatecAgent.chat(sessionId, message);
    }
}
