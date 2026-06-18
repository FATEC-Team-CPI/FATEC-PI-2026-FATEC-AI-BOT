# Plano de Time e Arquitetura AWS + AI

## 1) Contexto
Este documento consolida a proposta para o cenario:
- Frontend em Vue.
- Backend principal em Quarkus.
- Trilha de AI em Python em paralelo.
- Evolucao antecipada para AWS (LocalStack primeiro, depois Free Tier).

Distribuicao informada:
- 3 pessoas Frontend.
- 3 pessoas Backend.
- 2 pessoas AI.
- 2 pessoas Documentacao.

## 2) Direcao recomendada
Faz sentido adotar AWS cedo, desde que com entrada incremental:
1. Quarkus continua como servico central de negocio.
2. AI roda como servico Python separado.
3. S3 + DynamoDB + Step Functions entram por etapas, sem reescrever o dominio.

## 3) Divisao de responsabilidades por time

### 3.1 Frontend (3)
- FE-1: autenticacao, permissao e shell do admin.
- FE-2: cadastro/edicao/publicacao de conteudo, tags e upload.
- FE-3: chat UI, WebSocket, exibicao de fontes e estados do chat.

### 3.2 Backend Quarkus (3)
- BE-1: auth, RBAC, auditoria e usuarios admins.
- BE-2: conteudo, taxonomia/tags, publicacao, integracao com S3.
- BE-3: gateway de chat, disparo de ingestao (Step Functions), status de jobs.

### 3.3 AI Python (2)
- AI-1: ingestao de IA (extracao, chunking, embeddings).
- AI-2: retrieval, prompting, avaliacao de qualidade, guardrails e MCP tools.

### 3.4 Documentacao (2)
- DOC-1: ADRs, contratos de API, padroes de dominio e runbooks.
- DOC-2: onboarding, playbooks de operacao, guias de testes e release notes.

## 4) O que continua no Quarkus
- Auth e autorizacao.
- CRUD de conteudo e tags.
- Regras de negocio e auditoria.
- Endpoint de publicacao e disparo do pipeline de ingestao.
- WebSocket de chat para o frontend.
- Integracao com AI service Python.

## 5) Arquitetura de servicos (alto nivel)
1. Vue chama API REST e WebSocket no Quarkus.
2. Quarkus persiste metadados e estado em DynamoDB.
3. Quarkus salva/consulta documentos no S3.
4. Quarkus dispara Step Functions ao publicar/reindexar.
5. Step Functions orquestra Lambdas de ingestao.
6. AI Python atende retrieval e geracao de resposta.
7. Quarkus retorna resposta do chat com fontes.

## 6) S3 para documentos e artefatos

Bucket sugerido: `fatec-ai-content`

Estrutura de chaves:
- `raw/{contentId}/{version}/arquivo-original.ext`
- `processed/{contentId}/{version}/extracted.txt`
- `processed/{contentId}/{version}/chunks.jsonl`
- `processed/{contentId}/{version}/manifest.json`

Boas praticas:
- Habilitar versionamento do bucket.
- Criptografia server-side (KMS quando possivel).
- Politicas IAM por prefixo.

## 7) DynamoDB para metadados, sessao e estado

Tabela sugerida (single-table): `fatec_ai_core`

### 7.1 Conteudo
- `PK = CONTENT#{contentId}`
- `SK = META`
- Campos: `title`, `status`, `contentType`, `tags[]`, `s3KeyRaw`, `s3KeyText`, `publishedAt`, `createdBy`.

### 7.2 Versoes de conteudo
- `PK = CONTENT#{contentId}`
- `SK = VERSION#{timestamp}`
- Campos: snapshot, `versionNumber`.

### 7.3 Jobs de ingestao
- `PK = CONTENT#{contentId}`
- `SK = JOB#{jobId}`
- Campos: `jobStatus`, `step`, `error`, `startedAt`, `finishedAt`.

### 7.4 Sessao de chat
- `PK = SESSION#{sessionId}`
- `SK = META`
- Campos: `userId`, `createdAt`, `ttl`.

### 7.5 Mensagens de chat
- `PK = SESSION#{sessionId}`
- `SK = MSG#{timestamp}#{messageId}`
- Campos: `role`, `text`, `sources[]`, `latencyMs`, `ttl`.

### 7.6 Catalogo de tags
- `PK = TAG#{tagSlug}`
- `SK = META`
- Campos: `name`, `category`, `isActive`.

### 7.7 GSIs recomendadas
- GSI1 (conteudo por status/data):
  - `GSI1PK = CONTENT_STATUS#{status}`
  - `GSI1SK = {publishedAt}`
- GSI2 (conteudo por tag):
  - `GSI2PK = TAG#{tagSlug}`
  - `GSI2SK = CONTENT#{publishedAt}#{contentId}`
- GSI3 (jobs por status):
  - `GSI3PK = JOB_STATUS#{jobStatus}`
  - `GSI3SK = {createdAt}`

## 8) Pipeline de ingestao com Step Functions

Fluxo sugerido:
1. `StartIngestion` (chamado pelo Quarkus no publish/reindex).
2. `ValidateContent`.
3. `ExtractText`.
4. `ChunkText`.
5. `GenerateEmbeddings`.
6. `UpsertVectorIndex`.
7. `UpdateJobStatusDone`.

Boas praticas:
- Retry por etapa com backoff.
- `Catch` para estado `FAILED`.
- Status de job sempre refletido no backend.

## 9) Vetores e retrieval
- DynamoDB nao deve ser o mecanismo principal de similaridade vetorial.
- Opcoes recomendadas para vetor:
  - `pgvector` (se mantiver SQL para essa parte).
  - OpenSearch Vector.

Uso do DynamoDB no chat:
- Sessao, cache curto, estado e metadados.
- Nao como motor semantico principal.

## 10) Fluxo de chat ponta a ponta
1. Vue envia pergunta por WebSocket ao Quarkus.
2. Quarkus valida token e politicas.
3. Quarkus envia pergunta + filtros (tags/tipo) para AI Python.
4. AI executa retrieval + geracao.
5. Quarkus responde com `answer` e `sources[]`.
6. Historico resumido fica em DynamoDB (com TTL quando aplicavel).

## 11) MCP nesta arquitetura
E possivel e recomendado para a camada de AI.

Exemplos de tools MCP internas:
- `search_published_content(tag, contentType, dateRange)`
- `get_document_snippet(contentId, section)`
- `get_academic_calendar()`
- `trigger_reindex(contentId)` (com escopo e controle)

Regras de seguranca:
- MCP interno, nao exposto diretamente ao cliente final.
- Controle de permissao por ferramenta.
- Auditoria de chamadas e resultados.

## 12) Roadmap de adocao (AWS cedo, sem excesso)
1. Fase A: Quarkus + S3 + DynamoDB + AI Python (integracao direta).
2. Fase B: Step Functions + Lambdas na ingestao.
3. Fase C: vetor dedicado e avaliacao de qualidade.
4. Fase D: MCP tools com governanca e observabilidade completa.

## 13) Definicao de pronto por fase

### Fase A
- Upload no S3 funcionando.
- Conteudo e tags consultaveis no DynamoDB.
- Chat funcional com AI Python.

### Fase B
- Pipeline assinado por estado (`PENDING`, `RUNNING`, `DONE`, `FAILED`).
- Reprocessamento confiavel por `contentId`.

### Fase C
- Busca semantica com fontes relevantes.
- Metricas de qualidade (precision@k basico, taxa de fallback).

### Fase D
- Ferramentas MCP em producao com auditoria.
- Playbooks de incidente e operacao atualizados.

## 14) Desenho de dados por tecnologia

## 14.1 Mapa geral
1. Usuario/Admin -> Vue -> Quarkus.
2. Quarkus grava metadados operacionais no DynamoDB.
3. Quarkus grava documentos e artefatos no S3.
4. Pipeline escreve vetores e metadados de busca no OpenSearch.
5. AI Python consulta OpenSearch e devolve resposta com fontes.

## 14.2 Quarkus (dominio e orquestracao)
Dados de dominio tratados:
- autenticacao, autorizacao e papeis.
- conteudo, tags e estado de publicacao.
- disparo e acompanhamento de ingestao.
- sessao de chat e rastreio de fontes.

Identificadores canonicos gerados/propagados:
- `contentId`
- `version`
- `jobId`
- `sessionId`

## 14.3 S3 (fonte canonica de arquivos)
Objetos esperados:
- `raw/{contentId}/{version}/arquivo-original.ext`
- `processed/{contentId}/{version}/extracted.txt`
- `processed/{contentId}/{version}/chunks.jsonl`
- `processed/{contentId}/{version}/manifest.json`

Campos recomendados no `manifest.json`:
- `contentId`
- `version`
- `contentType`
- `tagSlugs`
- `chunkCount`
- `embeddingModel`
- `processedAt`

## 14.4 DynamoDB (estado e metadado operacional)
Tabela `fatec_ai_core`:
- metadados de conteudo e tags.
- estado de job de ingestao.
- sessao e historico resumido de chat.

Uso recomendado:
- DynamoDB para metadado e acesso por chave.
- nao usar DynamoDB como motor principal de similaridade vetorial.

## 14.5 OpenSearch (indice vetorial)
Indice sugerido: `fatec-content-vectors`.

Documento por chunk:
- `chunkId`
- `contentId`
- `version`
- `chunkText`
- `embedding`
- `contentType`
- `tagSlugs`
- `status`
- `sourceTitle`

Consulta tipica:
1. gerar embedding da pergunta.
2. executar knn no `embedding`.
3. aplicar filtro por `status=PUBLISHED`.
4. aplicar filtros opcionais por `tagSlugs` e `contentType`.
5. retornar `topK` chunks para a camada de AI.

## 14.6 AI Python (retrieval + geracao)
Entrada:
- `question`
- `sessionId`
- filtros opcionais (`tag`, `contentType`).

Processamento:
- retrieval no OpenSearch.
- montagem de contexto com fontes.
- chamada ao modelo LLM.

Saida:
- `answer`
- `sources[]` (com `contentId`, `sourceTitle`, trecho).
- metricas opcionais (`latencyMs`, `tokenUsage`).

## 14.7 Regra de consistencia
- S3: verdade do arquivo e artefato processado.
- DynamoDB: verdade do estado operacional.
- OpenSearch: verdade de busca semantica.
- Quarkus: verdade de regra de negocio e autorizacao.
