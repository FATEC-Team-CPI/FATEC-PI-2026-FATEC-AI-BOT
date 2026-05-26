# 🚀 Quick Start — FATEC AI Bot com Docker Compose

## 1️⃣ Preparação (5 min)

```bash
# Linux/Mac
cp .env.example .env
nano .env  # Edite e adicione sua GROQ_API_KEY

# Windows PowerShell
Copy-Item .env.example .env
notepad .env  # Edite e adicione sua GROQ_API_KEY
```

**Obter GROQ_API_KEY:** https://console.groq.com/keys

---

## 2️⃣ Iniciar Tudo (10 min)

### Linux/Mac (recomendado)
```bash
make compose-up
```

### Windows
```powershell
.\docker-compose.ps1 -Command up
```

### Qualquer SO (Docker Compose direto)
```bash
docker-compose build
docker-compose up -d
```

---

## 3️⃣ Verificar Status

```bash
# Linux/Mac
make compose-health

# Windows
.\docker-compose.ps1 -Command health

# Docker direto
docker-compose ps
```

Esperado:
```
NAME                   STATUS        PORTS
fatec-localstack       Up (healthy)  0.0.0.0:4566->4566/tcp
fatec-mcp-server       Up (healthy)  0.0.0.0:8001->8001/tcp
fatec-quarkus-backend  Up (healthy)  0.0.0.0:8082->8082/tcp
```

---

## 4️⃣ Acessar Serviços

| Serviço | URL | Descrição |
|---------|-----|-----------|
| **Backend** | http://localhost:8082 | API Quarkus |
| **Swagger** | http://localhost:8082/swagger | Documentação API |
| **WebSocket** | ws://localhost:8082/ws/chat | Chat |
| **MCP Server** | http://localhost:8001/health | Health check |
| **LocalStack** | http://localhost:4566 | DynamoDB |

---

## 5️⃣ Ver Logs

```bash
# Todos os serviços
make compose-logs

# Específico
make compose-logs-backend
make compose-logs-mcp
make compose-logs-localstack
```

---

## 6️⃣ Parar Tudo

```bash
# Linux/Mac
make compose-down

# Windows
.\docker-compose.ps1 -Command down

# Docker direto
docker-compose down
```

---

## 🔥 Problemas Comuns

### "GROQ_API_KEY não encontrada"
- Edite `.env` e adicione sua chave
- Reinicie: `docker-compose restart quarkus-backend`

### "MCP Server não conecta"
- Verifique: `curl http://localhost:8001/health`
- Ver logs: `make compose-logs-mcp`

### "Porta 8082 já em uso"
- Mude no `docker-compose.yml`: `ports: - "8083:8082"`

### Remover tudo e recomeçar
```bash
make compose-clean
make compose-up
```

---

## 📚 Documentação Completa

Ver [DOCKER.md](./DOCKER.md) para:
- Configuração detalhada
- Integração com Vector DB
- Deploy em produção
- Troubleshooting avançado

---

## ✅ Checklist de Sucesso

- [ ] `.env` criado com `GROQ_API_KEY`
- [ ] `docker-compose up -d` rodou sem erros
- [ ] `make compose-health` mostra 3 serviços ✅
- [ ] `curl http://localhost:8082/health` retorna JSON
- [ ] `curl http://localhost:8001/health` retorna JSON
- [ ] Swagger abre em http://localhost:8082/swagger
- [ ] WebSocket conecta em ws://localhost:8082/ws/chat

---

## 🎯 Próximo Passo

Depois que tudo rodar, implementar LangChain4j:
1. Adicionar dependências no `pom.xml`
2. Criar `FatecAgent.java` interface
3. Criar `FatecAgentConfig.java` configuração
4. Testar fluxo completo

Ver: [plano-fatec-chat-mcp.md](./plano-fatec-chat-mcp.md)
