package org.acme.chat.core.service;

import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.exception.InvalidRequestException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.chat.core.model.ChatInput;
import org.acme.chat.core.port.ChatGateway;
import org.acme.chat.core.port.ChatUseCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class ChatUseCaseImpl implements ChatUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ChatUseCaseImpl.class);

    private final ChatGateway chatGateway;
    private final KeywordJailbreakDetector keywordDetector;

    @Inject
    public ChatUseCaseImpl(
        ChatGateway chatGateway,
        KeywordJailbreakDetector keywordDetector
    ) {
        this.chatGateway = chatGateway;
        this.keywordDetector = keywordDetector;
    }

    @Override
    public String handle(ChatInput input) {
        if (input == null || input.message() == null || input.message().trim().isEmpty()) {
            return "Por favor, envie uma mensagem válida 😊";
        }

        String trimmedMessage = input.message().trim();
        long startNanos = System.nanoTime();

        try {
            if (keywordDetector.isJailbreak(trimmedMessage)) {
                long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
                logger.info("🛡️ Mensagem bloqueada pelo detector - Sessão: {}, duracaoMs={}", input.sessionId(), elapsedMs);
                return "Desculpe, mas só posso responder perguntas sobre a FATEC Itaquera. " +
                    "Tente perguntar sobre calendário, contatos, regulamento, grade curricular, edital ou horários.";
            }

            String resposta = chatGateway.chat(input.sessionId(), trimmedMessage);
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            logger.info("🤖 chatGateway.chat concluído - Sessão: {}, respostaTamanho={}, duracaoMs={}",
                    input.sessionId(),
                    resposta == null ? 0 : resposta.length(),
                    elapsedMs);
            return resposta;
        } catch (RateLimitException e) {
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            logger.warn("⏳ Rate limit no Groq - Sessão: {}, duracaoMs={}", input.sessionId(), elapsedMs, e);
            return "O Groq atingiu o limite de tokens do dia no momento. Tente novamente mais tarde.";
        } catch (InvalidRequestException e) {
            // The model failed to call a tool (tool_use_failed / failed_generation)
            // Log details and return a friendly message so the WebSocket doesn't crash.
            String detail = e.getMessage();
            if (detail != null && detail.length() > 800) {
                detail = detail.substring(0, 800) + "...";
            }
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            logger.error("❌ InvalidRequestException durante tool call - Sessão: {}, duracaoMs={}, detail={}",
                    input.sessionId(),
                    elapsedMs,
                    detail,
                    e);
            return "Desculpe — houve uma falha ao executar uma operação interna (busca). Tente reformular sua pergunta ou peça para listar os documentos disponíveis.";
        }
    }
}
