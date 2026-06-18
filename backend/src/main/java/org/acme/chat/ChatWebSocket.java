package org.acme.chat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import io.vertx.core.Vertx;
import org.acme.chat.core.model.ChatInput;
import org.acme.chat.core.port.ChatUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Endpoint WebSocket para chat com IA
 *
 * Simplicidade extrema: LangChain4j gerencia tudo
 * - Tool calling automático
 * - Memória de conversa por sessão
 * - Orquestração Groq → MCP
 */
@ServerEndpoint("/chat/{sessionId}")
@ApplicationScoped
public class ChatWebSocket {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocket.class);

    @Inject
    Vertx vertx;

    @Inject
    RequestContextRunner requestContextRunner;

    @Inject
    ChatUseCase chatUseCase;

    @OnOpen
    public void onOpen(Session session, @PathParam("sessionId") String sessionId) {
        logger.info("✅ Nova conexão WebSocket - Sessão: {}, ID: {}", sessionId, session.getId());
    }

    @OnMessage
    public void onMessage(
        String mensagem,
        Session session,
        @PathParam("sessionId") String sessionId
    ) {
        vertx.<String>executeBlocking(promise -> {
            try {
                String resposta = requestContextRunner.run(() -> processMessage(mensagem, sessionId));
                promise.complete(resposta);
            } catch (Exception e) {
                logger.error("❌ Erro ao processar mensagem - Sessão: {}", sessionId, e);
                promise.complete("{\"error\":\"Falha ao processar solicitação\"}");
            }
        }, result -> {
            if (result.succeeded()) {
                session.getAsyncRemote().sendText(result.result(), sendResult -> {
                    if (sendResult.isOK()) {
                        logger.info("✅ Resposta enviada - Sessão: {}", sessionId);
                    } else {
                        logger.error("❌ Erro ao enviar resposta - Sessão: {}", sessionId, sendResult.getException());
                    }
                });
            } else {
                logger.error("❌ Erro no processamento assíncrono - Sessão: {}", sessionId, result.cause());
                session.getAsyncRemote().sendText("{\"error\":\"Falha ao processar solicitação\"}");
            }
        });
    }

    private String processMessage(String mensagem, String sessionId) {
        if (mensagem == null || mensagem.trim().isEmpty()) {
            logger.warn("⚠️ Mensagem vazia/whitespace - Sessão: {}", sessionId);
        } else {
            logger.info("📩 Mensagem recebida - Sessão: {}, Msg: {}", sessionId, mensagem.trim());
        }

        return chatUseCase.handle(new ChatInput(sessionId, mensagem));
    }

    @OnClose
    public void onClose(Session session, @PathParam("sessionId") String sessionId) {
        logger.info("🔴 Conexão fechada - Sessão: {}, ID: {}", sessionId, session.getId());
    }

    @OnError
    public void onError(Session session, @PathParam("sessionId") String sessionId, Throwable throwable) {
        logger.error("⚠️ Erro na conexão WebSocket - Sessão: {}", sessionId, throwable);
    }

}