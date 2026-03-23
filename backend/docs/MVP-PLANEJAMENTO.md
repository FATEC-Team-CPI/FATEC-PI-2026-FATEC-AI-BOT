# MVP - Plataforma de Conteudo + Chat AI (FATEC)

## 1) Objetivo do MVP
Criar um backend Quarkus com:
- Area admin autenticada para cadastrar e publicar conteudos institucionais (paginas e documentos publicos da Fatec).
- Pipeline de ingestao para transformar conteudo publicado em base consultavel por IA.
- Chat AI em tempo real via WebSocket, respondendo com base nesses conteudos.

## 2) Escopo do MVP (o que entra)
- Autenticacao de admin com JWT.
- CRUD de conteudos (texto e upload de PDF como prioridade).
- Status de publicacao: `DRAFT`, `PUBLISHED`.
- Ingestao assincrona ao publicar conteudo.
- Chat WebSocket com resposta baseada em RAG.
- Log de auditoria basico (quem criou/alterou/publicou).

## 3) Fora do escopo inicial (deixar para fase 2)
- Multi-tenant e permissoes complexas.
- Versionamento avancado de documentos.
- Painel frontend completo (pode usar Postman/Swagger no MVP).
- OCR avancado para imagens escaneadas.
- Moderacao sofisticada e analytics detalhado.

## 4) Arquitetura proposta
- Quarkus REST para admin.
- Quarkus WebSocket para chat.
- PostgreSQL para dados transacionais.
- Vetor store para embeddings (opcao MVP: pgvector no mesmo Postgres).
- Integracao LLM por API (ex.: OpenAI, Azure OpenAI, Ollama local).
- Ingestao em background (fila simples por tabela de jobs).

Fluxo resumido:
1. Admin publica conteudo.
2. Sistema enfileira job de ingestao.
3. Job extrai texto, quebra em chunks, gera embedding e salva no vetor store.
4. Usuario pergunta no chat.
5. Sistema recupera chunks relevantes e monta prompt com contexto + fontes.
6. LLM responde e retorna com citacoes.

## 5) Modelo de dados minimo

### Tabela `admin_user`
- `id` (UUID)
- `name`
- `email` (unique)
- `password_hash`
- `role` (`SUPER_ADMIN`, `EDITOR`)
- `created_at`, `updated_at`

### Tabela `content`
- `id` (UUID)
- `title`
- `source_type` (`PAGE`, `DOCUMENT`)
- `body_text` (para paginas)
- `file_path` (para documentos)
- `status` (`DRAFT`, `PUBLISHED`)
- `published_at`
- `created_by`, `updated_by`
- `created_at`, `updated_at`

### Tabela `ingestion_job`
- `id` (UUID)
- `content_id`
- `status` (`PENDING`, `RUNNING`, `DONE`, `FAILED`)
- `error_message`
- `started_at`, `finished_at`

### Tabela `content_chunk`
- `id` (UUID)
- `content_id`
- `chunk_index`
- `chunk_text`
- `embedding` (vector)
- `metadata_json` (pagina, secao, etc.)

### Tabela `chat_message` (opcional no MVP, mas recomendado)
- `id` (UUID)
- `session_id`
- `author_type` (`USER`, `ASSISTANT`)
- `message_text`
- `sources_json`
- `created_at`

## 6) Endpoints e canais minimos

### Auth/Admin
- `POST /auth/login`
- `POST /admins` (somente SUPER_ADMIN)

### Conteudo
- `POST /contents`
- `GET /contents`
- `GET /contents/{id}`
- `PUT /contents/{id}`
- `POST /contents/{id}/publish`

### Upload
- `POST /contents/{id}/upload` (multipart para PDF)

### Ingestao
- `POST /contents/{id}/reindex`
- `GET /ingestion-jobs/{id}`

### Chat
- WebSocket `ws/chat`
- Mensagem entrada: `{ sessionId, question }`
- Mensagem saida: `{ answer, sources[] }`

## 7) Extensoes Quarkus sugeridas
Executar no projeto existente:

```bash
./mvnw quarkus:add-extension -Dextensions="rest-jackson,hibernate-orm-panache,jdbc-postgresql,flyway,smallrye-jwt,elytron-security,bouncycastle,websockets,scheduler"
```

No Windows PowerShell/CMD:

```powershell
mvnw.cmd quarkus:add-extension -Dextensions="rest-jackson,hibernate-orm-panache,jdbc-postgresql,flyway,smallrye-jwt,elytron-security,bouncycastle,websockets,scheduler"
```

Observacao:
- Para LLM/RAG, integrar por SDK HTTP ou biblioteca Java (ex.: LangChain4j) na camada de servico.

## 8) Plano de implementacao (3 sprints)

## Sprint 1 - Base de seguranca e conteudo
Meta:
- Auth JWT funcionando.
- CRUD de conteudo com status `DRAFT`/`PUBLISHED`.
- Migracoes Flyway.

Entregas:
- Entidades + repositorios + servicos de auth e content.
- Endpoint de login e protecao por role.
- Auditoria minima (`created_by`, `updated_by`).

Checklist de pronto:
- Login retorna JWT valido.
- Endpoint admin bloqueia usuario sem token/role.
- Conteudo pode ser criado e publicado.

## Sprint 2 - Ingestao e indexacao
Meta:
- Ao publicar, gerar job de ingestao.
- Criar chunks e embeddings.

Entregas:
- Leitura de texto de pagina e PDF.
- Chunking (ex.: 500 a 1000 chars com overlap).
- Persistencia dos vetores em `content_chunk`.

Checklist de pronto:
- Conteudo publicado gera job `DONE`.
- Consulta vetorial retorna chunks relevantes.

## Sprint 3 - Chat WebSocket com RAG
Meta:
- Chat em tempo real consultando base indexada.

Entregas:
- Endpoint WebSocket `ws/chat`.
- Pipeline: pergunta -> retrieval -> prompt -> LLM -> resposta.
- Resposta com fontes (`title`, `content_id`, trecho).

Checklist de pronto:
- Chat responde perguntas sobre conteudo da Fatec.
- Resposta inclui pelo menos 1 fonte quando houver contexto.

## 9) Regras de negocio recomendadas
- So conteudo `PUBLISHED` entra no RAG.
- Republicacao apaga chunks antigos e reindexa.
- Perguntas sem contexto relevante retornam fallback seguro.
- Sempre registrar fonte usada para auditoria.

## 10) Riscos e mitigacao
- Alucinacao da IA: usar prompt com restricao de dominio + citacao obrigatoria.
- Custo de embedding/LLM: limitar tamanho de chunk e numero de chunks por consulta.
- Documentos ruins: validar tipo/tamanho e registrar falhas de ingestao.

## 11) Definicao de sucesso do MVP
- Admin consegue cadastrar/publicar conteudo sem suporte tecnico.
- Conteudo publicado e indexado em ate 2 minutos (meta inicial).
- Chat responde com base no acervo publicado e exibe fontes.

## 12) Proximos passos imediatos
1. Criar branch `feature/mvp-auth-content-chat`.
2. Adicionar extensoes Quarkus e configuracoes de ambiente.
3. Subir PostgreSQL com pgvector.
4. Implementar Sprint 1 com testes de integracao basicos.

## 13) Documento complementar
- Taxonomia e tags de conteudo: `docs/TAXONOMIA-TAGS-CONTENT.md`
