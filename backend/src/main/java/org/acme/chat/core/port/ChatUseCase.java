package org.acme.chat.core.port;

import org.acme.chat.core.model.ChatInput;

public interface ChatUseCase {
    String handle(ChatInput input);
}
