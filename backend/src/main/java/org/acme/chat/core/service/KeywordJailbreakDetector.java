package org.acme.chat.core.service;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class KeywordJailbreakDetector {

    private static final List<String> JAILBREAK_PATTERNS = List.of(
        "ignora", "esqueça", "esqueca", "ignoring", "forget",
        "mude de role", "change role", "system prompt",
        "você é", "voce e", "você é agora", "voce e agora", "pretend", "atuar como",
        "nova instrução", "nova instrucao", "nova ordem", "nova regra",
        "esqueça as instruções", "esqueca as instrucoes", "ignore as instruções", "ignore as instrucoes",
        "system message", "system instruction"
    );

    public boolean isJailbreak(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        String lower = message.toLowerCase();
        return JAILBREAK_PATTERNS.stream().anyMatch(lower::contains);
    }
}
