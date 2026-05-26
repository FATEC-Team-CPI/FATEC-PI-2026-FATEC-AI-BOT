package org.acme.chat.core.port;

public interface SemanticJailbreakDetector {
    boolean isJailbreak(String message);
}
