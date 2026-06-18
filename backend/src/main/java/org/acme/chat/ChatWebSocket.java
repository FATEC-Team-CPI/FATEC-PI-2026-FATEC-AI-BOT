package org.acme.chat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.acme.chat.core.model.ChatInput;
import org.acme.chat.core.port.ChatUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Endpoint WebSocket para chat com IA
 *
 * Simplicidade extrema: LangChain4j gerencia tudo
 * - Tool calling automático
 * - Memória de conversa por sessão
 * - Orquestração Groq → MCP
 */
@ServerEndpoint("/ws/chat/{sessionId}")
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
        logger.info("↳ WebSocket aberto - open={}, maxIdleTimeout={}, maxBinaryBufferSize={}, maxTextBufferSize={}",
                session.isOpen(),
                session.getMaxIdleTimeout(),
                session.getMaxBinaryMessageBufferSize(),
                session.getMaxTextMessageBufferSize());
    }

    @OnMessage
    public void onMessage(
        String mensagem,
        Session session,
        @PathParam("sessionId") String sessionId
    ) {
        long receivedAtNanos = System.nanoTime();
        logger.info("📥 Mensagem recebida no socket - Sessão: {}, ID: {}, tamanho: {}, open={}",
                sessionId,
                session.getId(),
                mensagem == null ? 0 : mensagem.length(),
                session.isOpen());

        Context context = vertx.getOrCreateContext();
        Future<String> future = context.executeBlocking(() -> {
            try {
                long processingStartNanos = System.nanoTime();
                logger.info("⏳ Iniciando processamento de mensagem - Sessão: {}", sessionId);
                String resposta = requestContextRunner.run(() -> processMessage(mensagem, sessionId));
                long processingElapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - processingStartNanos);
                logger.info("✅ Processamento finalizado - Sessão: {}, respostaTamanho={}, duracaoMs={}",
                        sessionId,
                        resposta == null ? 0 : resposta.length(),
                        processingElapsedMs);
                return resposta;
            } catch (Exception e) {
                logger.error("❌ Erro ao processar mensagem - Sessão: {}", sessionId, e);
                return "{\"error\":\"Falha ao processar solicitação\"}";
            }
        });

        future.onComplete(result -> {
            long totalElapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - receivedAtNanos);
            if (result.succeeded()) {
                logger.info("📤 Enviando resposta ao socket - Sessão: {}, open={}, duracaoTotalMs={}",
                        sessionId,
                        session.isOpen(),
                        totalElapsedMs);
                session.getAsyncRemote().sendText(result.result(), sendResult -> {
                    if (sendResult.isOK()) {
                        logger.info("✅ Resposta enviada - Sessão: {}, duracaoTotalMs={}", sessionId, totalElapsedMs);
                    } else {
                        logger.error("❌ Erro ao enviar resposta - Sessão: {}", sessionId, sendResult.getException());
                    }
                });
            } else {
                logger.error("❌ Erro no processamento assíncrono - Sessão: {}, duracaoTotalMs={}", sessionId, totalElapsedMs, result.cause());
                session.getAsyncRemote().sendText("{\"error\":\"Falha ao processar solicitação\"}");
            }
        });
    }

    private String processMessage(String mensagem, String sessionId) {
        long handleStartNanos = System.nanoTime();
        if (mensagem == null || mensagem.trim().isEmpty()) {
            logger.warn("⚠️ Mensagem vazia/whitespace - Sessão: {}", sessionId);
        } else {
            logger.info("📩 Mensagem recebida - Sessão: {}, Msg: {}", sessionId, mensagem.trim());
        }

        String resposta = chatUseCase.handle(new ChatInput(sessionId, mensagem));
        long handleElapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - handleStartNanos);
        logger.info("🧠 chatUseCase.handle concluído - Sessão: {}, respostaTamanho={}",
                sessionId,
                resposta == null ? 0 : resposta.length());
        logger.info("⏱️ Tempo do chatUseCase.handle - Sessão: {}, duracaoMs={}", sessionId, handleElapsedMs);
        return resposta;
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason, @PathParam("sessionId") String sessionId) {
        logger.info("🔴 Conexão fechada - Sessão: {}, ID: {}, code={}, reason={}, open={}",
                sessionId,
                session.getId(),
                closeReason.getCloseCode(),
                closeReason.getReasonPhrase(),
                session.isOpen());
    }

    @OnError
    public void onError(Session session, @PathParam("sessionId") String sessionId, Throwable throwable) {
        logger.error("⚠️ Erro na conexão WebSocket - Sessão: {}, open={}, id={}",
                sessionId,
                session != null && session.isOpen(),
                session != null ? session.getId() : "null");
        logger.error("⚠️ Stacktrace da conexão WebSocket - Sessão: {}", sessionId, throwable);
    }

}