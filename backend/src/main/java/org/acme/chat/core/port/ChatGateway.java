package org.acme.chat.core.port;

public interface ChatGateway {
    String chat(String sessionId, String message);
}
