package org.acme.ai;

import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuração do FatecAgent
 *
 * Conecta:
 * 1. Groq LLM (llama-3.3-70b-versatile)
 * 2. Tools locais que chamam MCP Server Python via HTTP
 * 3. Memória de conversa por sessionId
 *
 * LangChain4j gerencia automaticamente:
 * - Loop de tool calling (pergunta → Groq → tools → resposta)
 * - Histórico de mensagens por usuário
 */
@ApplicationScoped
public class FatecAgentConfig {

    private static final Logger logger = LoggerFactory.getLogger(FatecAgentConfig.class);

    @ConfigProperty(name = "groq.api.key")
    String groqApiKey;


    /**
     * Produz um modelo separado APENAS para validação de jailbreak
     * Usa temperatura 0 para ser determinístico e rígido
     */
    @Produces
    @Singleton
    @jakarta.inject.Named("validationModel")
    public OpenAiChatModel validationModel() {
        logger.info("🔒 Inicializando modelo de validação (temperatura 0)...");
        var model = OpenAiChatModel.builder()
            .baseUrl("https://api.groq.com/openai/v1")
            .apiKey(groqApiKey)
            .modelName("llama-3.3-70b-versatile")
            .temperature(0.0) // Determinístico para validação
            .timeout(java.time.Duration.ofSeconds(5))
            .build();
        logger.info("✅ Modelo de validação pronto");
        return model;
    }

}
