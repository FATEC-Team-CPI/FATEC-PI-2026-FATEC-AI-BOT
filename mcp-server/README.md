# MCP Server — FATEC AI Bot

Servidor MCP (Model Context Protocol) que expõe ferramentas de busca em documentos institucionais da FATEC Itaquera para o backend Quarkus.

## 🏗️ Arquitetura

```
Quarkus Backend (LangChain4j)
        ↓ HTTP/SSE
MCP Server Python (FastMCP)
        ↓
Vector DB (ChromaDB / Qdrant / pgvector)
```

## 🛠️ Tools Disponíveis

### 1. `search_fatec_itaquera_site(query, max_pages)`
Busca informações no site oficial da FATEC Itaquera.

**Uso recomendado:**
- páginas institucionais
- notícias
- endereço e contatos
- cursos e informações publicadas no site

**Resposta:**
```json
[
  {
    "source": "Fatec Itaquera Official Site",
    "url": "https://fatecitaquera.cps.sp.gov.br/...",
    "title": "...",
    "description": "...",
    "excerpt": "...",
    "relevance": 3
  }
]
```

### 2. `list_available_documents()`
Lista todos os documentos disponíveis na base de conhecimento.

**Resposta:**
```json
[
  {
    "id": "doc_001",
    "name": "Calendário Acadêmico 2025",
    "type": "calendario_academico",
    "description": "Datas de aulas, provas, recessos e férias",
    "updated_at": "2025-05-22T10:30:00"
  },
  ...
]
```

### 3. `search_fatec_documents(query, document_type)`
Busca informações dentro de um documento específico.

**Parâmetros:**
- `query` (string): Pergunta ou termo de busca
- `document_type` (string): Tipo do documento (calendario_academico, edital_vestibular, grade_curricular, regulamento, contato)

**Resposta:**
```json
[
  {
    "content": "Recesso de julho: 14/07 a 18/07/2025",
    "source": "Calendário Acadêmico 2025",
    "document_type": "calendario_academico",
    "relevance": 0.98
  },
  ...
]
```

### 4. `health_check()`
Verifica saúde do servidor MCP.

---

## 📦 Instalação

### Pré-requisitos
- Python 3.9+
- pip ou uv

### Setup

1. **Clonar/criar pasta mcp-server** (já feito)

2. **Criar ambiente virtual:**
```bash
cd mcp-server
python -m venv venv

# Windows
venv\Scripts\activate

# Linux/Mac
source venv/bin/activate
```

3. **Instalar dependências:**
```bash
pip install -r requirements.txt
```

4. **Configurar variáveis de ambiente:**
```bash
cp .env.example .env
# Editar .env conforme necessário
```

---

## 🚀 Execução

### Modo Desenvolvimento
```bash
python mcp_server.py
```

Saída esperada:
```
🚀 Iniciando MCP Server FATEC...
📡 Expondo tools via HTTP/SSE na porta 8001
   - list_available_documents()
   - search_fatec_documents(query, document_type)
  - search_fatec_itaquera_site(query, max_pages)
   - health_check()
```

### Modo Produção (com Gunicorn)
```bash
gunicorn -w 4 -k uvicorn.workers.UvicornWorker --bind 0.0.0.0:8001 mcp_server:mcp
```

---

## 🔗 Integração com Quarkus

No `FatecAgentConfig.java`:

```java
McpClient mcpClient = new DefaultMcpClient.Builder()
    .transport(new HttpMcpTransport.Builder()
        .sseUrl("http://localhost:8001/sse")  // ← URL do MCP Server
        .build())
    .build();
```

---

## 📝 Categorias de Documentos

| `document_type` | Descrição |
|---|---|
| `calendario_academico` | Datas de aulas, provas, recessos, férias |
| `edital_vestibular` | Processo seletivo, inscrições, vagas |
| `grade_curricular` | Disciplinas por curso e semestre |
| `regulamento` | Normas, regras e regulamentos internos |
| `contato` | Endereço, telefones, e-mails, horários |

---

## 🗂️ Integração com Vector DB

### ChromaDB (Recomendado para MVP)
```bash
pip install chromadb
```

Código:
```python
import chromadb

client = chromadb.Client()
collection = client.get_or_create_collection("fatec-documents")

# Adicionar documentos
collection.add(
    documents=["texto do documento"],
    metadatas=[{"type": "calendario_academico"}],
    ids=["doc_001"]
)

# Buscar
results = collection.query(
    query_texts=["férias julho"],
    n_results=5
)
```

### Qdrant (Produção)
```bash
pip install qdrant-client
```

### PostgreSQL + pgvector
```bash
pip install psycopg pgvector
```

---

## 📂 Estrutura Esperada (próximas fases)

```
mcp-server/
├── mcp_server.py              # ← Servidor principal (atual)
├── requirements.txt
├── .env.example
├── README.md
├── vector_store/              # ← Integração com Vector DB
│   ├── __init__.py
│   ├── chroma_store.py        # ChromaDB
│   ├── qdrant_store.py        # Qdrant
│   └── pgvector_store.py      # PostgreSQL + pgvector
├── ingest/                    # ← Pipeline de ingestão
│   ├── __init__.py
│   ├── document_processor.py  # Processa PDFs/Word/etc
│   └── embeddings.py          # Gera embeddings
├── models/                    # ← DTOs
│   ├── __init__.py
│   ├── document.py
│   └── search_result.py
└── tests/                     # ← Testes
    ├── test_mcp_server.py
    └── test_tools.py
```

---

## 🧪 Testando as Tools

### Via curl (health check)
```bash
curl http://localhost:8001/health
```

### Via Python
```python
import httpx
import json

async with httpx.AsyncClient() as client:
    # List documents
    response = await client.post(
        "http://localhost:8001/tools/list_available_documents"
    )
    docs = response.json()
    print(docs)
```

---

## 📋 Próximas Fases

1. **Integrar Vector DB real** (ChromaDB/Qdrant/pgvector)
2. **Criar pipeline de ingestão** (upload de PDFs, processamento)
3. **Implementar busca semântica** (embeddings com SentenceTransformers)
4. **Autenticação** (JWT entre Quarkus e MCP)
5. **Caching** (Redis para resultados frequentes)
6. **Monitoring** (Prometheus/Grafana)

---

## 🐛 Troubleshooting

### MCP Server não conecta
- Verificar se está rodando: `curl http://localhost:8001/health`
- Verificar firewall porta 8001
- Logs do servidor MCP

### Busca retorna vazio
- Verificar `document_type` está correto
- Documento foi carregado no Vector DB?
- Verificar logs do servidor MCP

### Erro de import
```bash
pip install -r requirements.txt --force-reinstall
```

---

## 📞 Contato & Suporte

Para dúvidas sobre o MCP Server FATEC, consulte o plano em `../plano-fatec-chat-mcp.md`
