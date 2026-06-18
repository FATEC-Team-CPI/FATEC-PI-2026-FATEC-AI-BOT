# FATEC-PI-2026-FATEC-AI-BOT
Projeto integrador voltado a auxiliar estudantes e interessados na FATEC com auxílio e integração de agentes de IA.

## Integração local com Docker Compose

O repositório agora inclui uma composição local na raiz para subir os serviços juntos:

- `frontend` em `http://localhost:5173`
- `backend` em `http://localhost:8082`
- `mcp-server` em `http://localhost:8001`
- `localstack` em `http://localhost:4566`

Para subir tudo:

```bash
docker compose up --build
```

Para parar e remover os volumes locais:

```bash
docker compose down -v
```
