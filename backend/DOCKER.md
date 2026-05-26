# 🐳 Docker Compose — FATEC AI Bot

Guia completo para rodar o projeto inteiro com Docker Compose.

---

## 📋 Serviços

| Serviço | Porta | Descrição |
|---------|-------|-----------|
| **LocalStack** | 4566 | AWS Mock (DynamoDB) |
| **MCP Server** | 8001 | Python MCP Server (Ferramentas de Busca) |
| **Quarkus Backend** | 8082 | API + WebSocket + Chat |

---

## 🚀 Quick Start

### 1. **Preparar variáveis de ambiente**

```bash
cp .env.example .env
# Editar .env e adicionar sua GROQ_API_KEY
```

### 2. **Build e execução**

```bash
# Build das imagens
docker-compose build

# Subir os containers
docker-compose up -d

# Ver logs
docker-compose logs -f

# Parar
docker-compose down
```

### 3. **Verificar status**

```bash
docker-compose ps
```

Saída esperada:
```
NAME                   STATUS              PORTS
fatec-localstack       Up (healthy)        0.0.0.0:4566->4566/tcp
fatec-mcp-server       Up (healthy)        0.0.0.0:8001->8001/tcp
fatec-quarkus-backend  Up (healthy)        0.0.0.0:8082->8082/tcp
```

### 4. **Testar endpoints**

```bash
# Health check do MCP Server
curl http://localhost:8001/health

# Health check do Quarkus
curl http://localhost:8082/health

# Swagger UI do Quarkus
open http://localhost:8082/swagger

# WebSocket do Chat
ws://localhost:8082/ws/chat
```

---

## ⚙️ Configuração Detalhada

### LocalStack

```yaml
localstack:
  image: localstack/localstack-pro
  ports:
    - "4566:4566"
  environment:
    - SERVICES=dynamodb
    - AWS_DEFAULT_REGION=us-east-1
```

**Variáveis importantes no `.env`:**
- `LOCALSTACK_AUTH_TOKEN` — Token de autenticação
- `DDB_TABLE_NAME` — Nome da tabela DynamoDB

### MCP Server

```yaml
mcp-server:
  build:
    context: ./mcp-server
  ports:
    - "8001:8001"
  depends_on:
    - localstack
```

**Build automático** a partir de `./mcp-server/Dockerfile`

**Tools disponíveis:**
- `list_available_documents()`
- `search_fatec_documents(query, document_type)`
- `health_check()`

### Quarkus Backend

```yaml
quarkus-backend:
  build:
    context: .
    dockerfile: Dockerfile.dev
  ports:
    - "8082:8082"
  depends_on:
    - localstack
    - mcp-server
```

**Build automático** a partir de `./Dockerfile.dev`

**Variáveis de ambiente injetadas:**
- `GROQ_API_KEY` — API Key do Groq
- `MCP_SERVER_URL=http://mcp-server:8001` — URL interna para MCP Server
- `AWS_DYNAMODB_ENDPOINT_OVERRIDE=http://localstack:4566` — DynamoDB local

---

## 🔗 Conectividade Interna

Os serviços se comunicam pela rede `fatec-network`:

```
Quarkus (8082)
  ↓ HTTP/SSE
MCP Server (8001)
  ↓
LocalStack (4566)
```

**URLs internas** (dentro dos containers):
- MCP Server: `http://mcp-server:8001`
- LocalStack: `http://localstack:4566`

**URLs externas** (do seu computador):
- MCP Server: `http://localhost:8001`
- LocalStack: `http://localhost:4566`
- Quarkus: `http://localhost:8082`

---

## 📝 Usar Variáveis de Ambiente

### .env

```env
GROQ_API_KEY=gsk_seu_key_aqui
MCP_SERVER_URL=http://mcp-server:8001
AWS_DYNAMODB_ENDPOINT_OVERRIDE=http://localstack:4566
```

### Acessar no Docker Compose

```yaml
environment:
  - GROQ_API_KEY=${GROQ_API_KEY}
  - MCP_SERVER_URL=${MCP_SERVER_URL}
```

---

## 🛠️ Comandos Úteis

### Iniciar apenas um serviço

```bash
# Apenas LocalStack
docker-compose up localstack -d

# Apenas MCP Server
docker-compose up mcp-server -d

# Apenas Quarkus
docker-compose up quarkus-backend -d
```

### Ver logs de um serviço

```bash
docker-compose logs -f mcp-server
docker-compose logs -f quarkus-backend
docker-compose logs -f localstack
```

### Limpar tudo

```bash
docker-compose down -v  # Remove volumes também
```

### Rebuild de um serviço

```bash
docker-compose build mcp-server
docker-compose build quarkus-backend
```

### Acessar shell do container

```bash
docker-compose exec quarkus-backend bash
docker-compose exec mcp-server bash
```

---

## 🐛 Troubleshooting

### MCP Server não inicia

```bash
docker-compose logs mcp-server
```

Verificar se Python 3.11+ está instalado no Dockerfile.

### Quarkus não conecta ao MCP Server

1. Verificar se MCP Server está saudável:
```bash
docker-compose logs mcp-server | grep "health"
```

2. Verificar conectividade:
```bash
docker-compose exec quarkus-backend curl http://mcp-server:8001/health
```

### LocalStack não inicializa DynamoDB

```bash
docker-compose logs localstack | grep -i "dynamodb"
```

Verificar se `SERVICES=dynamodb` está no `.env`.

### Porta já em uso

```bash
# Mudar portas no docker-compose.yml
ports:
  - "8082:8082"  # Mudar primeira porta
```

---

## 📊 Estrutura de Volumes

| Volume | Path | Descrição |
|--------|------|-----------|
| `localstack-data` | `/var/lib/localstack` | Dados persistentes do LocalStack |
| `maven-cache` | `/root/.m2` | Cache Maven (builds mais rápidos) |
| `./mcp-server/data` | `/app/data` | Dados do MCP Server |
| `./src` | `/app/src` | Código-fonte (sync em dev) |
| `./target` | `/app/target` | Artifacts Maven (sync) |

---

## 🔐 Segurança

### Credenciais LocalStack

```env
LOCALSTACK_AUTH_TOKEN=ls-TALI7590-FAJa-mUnA-1658-JIpApIQO1156
```

**ATENÇÃO:** Este token é fictício para desenvolvimento. Em produção, usar credentials reais.

### Groq API Key

```env
GROQ_API_KEY=gsk_seu_key_aqui
```

**NUNCA** fazer commit do `.env` com chaves reais. Usar `.env.example` como template.

---

## 🚀 Deploy em Produção

Para produção, modificar:

1. **docker-compose.prod.yml**
```bash
version: '3.9'

services:
  quarkus-backend:
    image: seu-registry/fatec-quarkus:v1.0.0
    # Sem volumes de desenvolvimento
    # Build pré-feito, não build inline
```

2. **Execução**
```bash
docker-compose -f docker-compose.prod.yml up -d
```

---

## 📚 Referências

- [LocalStack Documentation](https://docs.localstack.cloud/)
- [MCP Protocol](https://modelcontextprotocol.io/)
- [Docker Compose](https://docs.docker.com/compose/)
- [Quarkus with Docker](https://quarkus.io/guides/deploying-to-kubernetes#using-dockerfile)

---

## ❓ Próximos Passos

1. Implementar `FatecAgent.java` e `FatecAgentConfig.java`
2. Integrar LangChain4j
3. Adicionar Vector DB (ChromaDB/Qdrant) ao docker-compose
4. Autenticação entre Quarkus e MCP Server
5. CI/CD pipeline (GitHub Actions)
