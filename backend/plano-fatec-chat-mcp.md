# Plano de Arquitetura — FATEC Itaquera Chat com IA

## Visão Geral

Chatbot inteligente para estudantes e interessados na FATEC Itaquera, utilizando **Groq** como LLM, **LangChain4j** como orquestrador no Quarkus e um **MCP Server Python** para busca nos documentos institucionais.

---

## Arquitetura Final

```
Estudante
    ↓ WebSocket
Quarkus (WebSocket + LangChain4j)
    ↓ Groq API (Llama 3.3 70B)
    ↓ MCP Client (automático via LangChain4j)
MCP Server Python
    ↓ Busca semântica
Vector DB (documentos ingeridos)
    ↑
Microserviço Python de Ingestão
    ↑
Storage de arquivos (upload via CRUD Quarkus)
```

---

## Estrutura de Repositório

```
fatec-chat/
├── quarkus-backend/
│   ├── src/main/java/
│   │   ├── ChatWebSocket.java        ← WebSocket (simples, ~15 linhas)
│   │   ├── FatecAgent.java           ← Interface do agente LangChain4j
│   │   └── FatecAgentConfig.java     ← Configuração Groq + MCP
│   └── src/main/resources/
│       └── application.properties
│
└── python-microservice/
    ├── ingestion/                    ← já existe (processa docs)
    ├── vector_store/                 ← ChromaDB / pgvector / Qdrant
    └── mcp_server.py                 ← NOVO: expõe tools via HTTP/SSE
```

---

## Fluxo de uma Dúvida

```
1. Estudante pergunta: "Quando são as férias de julho?"

2. LangChain4j envia para o Groq com as tools disponíveis

3. Groq chama → list_available_documents()
   MCP retorna → ["calendario_academico", "edital_vestibular", "grade_ADS", ...]

4. Groq analisa → pergunta sobre férias → documento: calendario_academico

5. Groq chama → search_fatec_documents(
       query="férias julho recesso",
       document_type="calendario_academico"
   )
   MCP retorna → "Recesso de julho: 14/07 a 18/07/2025..."

6. Groq responde → "As férias de julho na FATEC Itaquera são de 14 a 18 de julho de 2025."

7. Quarkus envia resposta ao estudante via WebSocket
```

---

## Componentes

### 1. MCP Server Python

Adicionar ao microserviço Python existente. Expõe duas tools que o Groq pode chamar.

```python
from mcp.server.fastmcp import FastMCP

mcp = FastMCP("fatec-docs")

@mcp.tool()
async def list_available_documents() -> list[dict]:
    """
    Lista todos os documentos disponíveis na base de conhecimento
    da FATEC Itaquera. SEMPRE chame esta função primeiro, antes de
    buscar, para identificar qual documento é mais relevante.
    """
    documents = await document_repository.list_all()
    return [
        {
            "id": doc.id,
            "name": doc.name,
            "type": doc.type,
            "description": doc.description,
            "updated_at": doc.updated_at
        }
        for doc in documents
    ]

@mcp.tool()
async def search_fatec_documents(
    query: str,
    document_type: str
) -> list[dict]:
    """
    Busca informações dentro de um documento da FATEC Itaquera.
    Só chame após usar list_available_documents() para identificar
    o document_type correto.
    """
    results = await vector_db.search(
        query=query,
        filter={"type": document_type},
        limit=5
    )
    return [
        {
            "content": r.text,
            "source": r.filename,
            "relevance": r.score
        }
        for r in results
    ]

# Expõe via HTTP/SSE para o Quarkus
mcp.run(transport="streamable-http", port=8001)
```

---

### 2. Dependências Quarkus (`pom.xml`)

```xml
<!-- LangChain4j com Groq (compatível com OpenAI) -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>0.36.0</version>
</dependency>

<!-- LangChain4j core -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>0.36.0</version>
</dependency>

<!-- MCP Client -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-mcp</artifactId>
    <version>0.36.0</version>
</dependency>
```

---

### 3. Interface do Agent (`FatecAgent.java`)

```java
// O LangChain4j implementa automaticamente
public interface FatecAgent {
    String chat(@MemoryId String sessionId, @UserMessage String message);
}
```

---

### 4. Configuração do Agent (`FatecAgentConfig.java`)

```java
@ApplicationScoped
public class FatecAgentConfig {

    @ConfigProperty(name = "groq.api.key")
    String groqApiKey;

    @ConfigProperty(name = "mcp.server.url")
    String mcpServerUrl;

    private static final String SYSTEM_PROMPT = """
        Você é um assistente virtual da FATEC Itaquera.

        Quando receber uma dúvida, siga SEMPRE esta ordem:
        1. Chame list_available_documents() para ver o que está disponível.
        2. Identifique qual documento atende melhor a dúvida.
        3. Chame search_fatec_documents() com o document_type escolhido.
        4. Responda com base no conteúdo encontrado.

        Se nenhum documento for relevante, informe que não há
        informação disponível sobre o assunto.
        Responda sempre em português, de forma clara e objetiva.
        """;

    @Produces
    @ApplicationScoped
    public FatecAgent fatecAgent() {

        // Conecta ao MCP Server Python via SSE
        McpClient mcpClient = new DefaultMcpClient.Builder()
            .transport(new HttpMcpTransport.Builder()
                .sseUrl(mcpServerUrl + "/sse")
                .build())
            .build();

        // Carrega as tools do MCP Server automaticamente
        McpToolProvider toolProvider = McpToolProvider.builder()
            .mcpClients(mcpClient)
            .build();

        // Configura o modelo Groq
        ChatLanguageModel groq = OpenAiChatModel.builder()
            .baseUrl("https://api.groq.com/openai/v1")
            .apiKey(groqApiKey)
            .modelName("llama-3.3-70b-versatile")
            .build();

        // Cria o Agent com memória de conversa por sessão
        return AiServices.builder(FatecAgent.class)
            .chatLanguageModel(groq)
            .toolProvider(toolProvider)
            .chatMemoryProvider(
                sessionId -> MessageWindowChatMemory.withMaxMessages(20)
            )
            .systemMessageProvider(id -> SYSTEM_PROMPT)
            .build();
    }
}
```

---

### 5. WebSocket (`ChatWebSocket.java`)

```java
@ServerEndpoint("/chat/{sessionId}")
@ApplicationScoped
public class ChatWebSocket {

    @Inject
    FatecAgent fatecAgent;

    @OnMessage
    public void onMessage(String message,
                          Session session,
                          @PathParam("sessionId") String sessionId) {

        // Uma linha — LangChain4j gerencia o loop de tools internamente
        String resposta = fatecAgent.chat(sessionId, message);

        session.getAsyncRemote().sendText(resposta);
    }

    @OnClose
    public void onClose(@PathParam("sessionId") String sessionId) {
        // Memória é gerenciada pelo LangChain4j
    }
}
```

---

### 6. `application.properties`

```properties
groq.api.key=${GROQ_API_KEY}
mcp.server.url=http://localhost:8001
```

---

## Categorias de Documentos Sugeridas

| Tipo (`document_type`) | Descrição |
|---|---|
| `calendario_academico` | Datas de aulas, provas, recessos, férias |
| `edital_vestibular` | Processo seletivo, inscrições, vagas |
| `grade_curricular` | Disciplinas por curso e semestre |
| `regulamento` | Normas, regras e regulamentos internos |
| `contato` | Endereço, telefones, e-mails, horários |

---

## Comparativo: Antes vs Depois

| | Antes (sem LangChain4j) | Depois (com LangChain4j) |
|---|---|---|
| Loop de tool_calls | Implementado manualmente | Automático |
| Histórico de conversa | Gerenciado manualmente | Automático por sessão |
| Descoberta de tools MCP | Manual | Automático via SSE |
| Código no WebSocket | ~100 linhas | ~15 linhas |
| Lógica de orquestração | No Quarkus | No LangChain4j |

---

## Limites do Groq Free Tier

| Métrica | Limite |
|---|---|
| Requisições por minuto (RPM) | 30 |
| Tokens por minuto (TPM) | 6.000 |
| Requisições por dia (RPD) | 14.400 |
| Modelo recomendado | `llama-3.3-70b-versatile` |

> Para um chatbot universitário com volume moderado, o free tier do Groq cobre bem o MVP.

---

## Próximos Passos

1. Adicionar `mcp_server.py` ao microserviço Python existente
2. Escolher e configurar o Vector DB (ChromaDB é o mais simples para começar)
3. Adicionar dependências LangChain4j no `pom.xml`
4. Implementar `FatecAgentConfig.java` e `FatecAgent.java`
5. Simplificar o `ChatWebSocket.java` existente
6. Testar o fluxo completo de duas etapas (list → search → resposta)
