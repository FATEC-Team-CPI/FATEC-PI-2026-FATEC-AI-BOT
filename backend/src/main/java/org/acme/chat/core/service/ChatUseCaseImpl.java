package org.acme.chat.core.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.chat.core.model.ChatInput;
import org.acme.chat.core.port.ChatGateway;
import org.acme.chat.core.port.ChatUseCase;
import org.acme.chat.core.port.SemanticJailbreakDetector;

@ApplicationScoped
public class ChatUseCaseImpl implements ChatUseCase {

    private final ChatGateway chatGateway;
    private final SemanticJailbreakDetector semanticDetector;
    private final KeywordJailbreakDetector keywordDetector;

    @Inject
    public ChatUseCaseImpl(
        ChatGateway chatGateway,
        SemanticJailbreakDetector semanticDetector,
        KeywordJailbreakDetector keywordDetector
    ) {
        this.chatGateway = chatGateway;
        this.semanticDetector = semanticDetector;
        this.keywordDetector = keywordDetector;
    }

    @Override
    public String handle(ChatInput input) {
        if (input == null || input.message() == null || input.message().trim().isEmpty()) {
            return "Por favor, envie uma mensagem válida 😊";
        }

        String trimmedMessage = input.message().trim();

        if (keywordDetector.isJailbreak(trimmedMessage)) {
            return "Desculpe, mas só posso responder perguntas sobre a FATEC Itaquera. " +
                "Tente perguntar sobre calendário, contatos, regulamento, grade curricular, edital ou horários.";
        }

        if (semanticDetector.isJailbreak(trimmedMessage)) {
            return "Obrigado por usar o assistente FATEC. Por favor, faça perguntas relacionadas aos serviços da universidade.";
        }

        return chatGateway.chat(input.sessionId(), trimmedMessage);
    }
}
