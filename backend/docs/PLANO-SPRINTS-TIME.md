# Plano de Sprints do Time

## 1) Contexto atual informado
Sprint em andamento:
1. Criacao dos ambientes no GitHub (ja concluido).
2. Criacao de autenticacao no backend e frontend (login/cadastro).
3. Criacao das telas no Figma (frontend).
4. Estudo da parte de AI e tecnologias de banco (AI/DB).

Composicao do time:
- 3 Frontend (Vue)
- 3 Backend (Quarkus)
- 2 AI (Python)
- 2 Documentacao

## 2) Cadencia sugerida
- Sprint de 2 semanas.
- Planejamento no dia 1.
- Review e retro no ultimo dia.
- Um checkpoint tecnico no meio da sprint.

## 3) Sprint 0 (atual) - Fundacao

Meta:
- Preparar base de colaboracao, identidade visual inicial e trilhas tecnicas.

Entregas esperadas:
- Ambientes GitHub prontos.
- Auth base em andamento (back e front).
- Protótipos principais no Figma.
- Documento de decisao de stack AI/DB inicial.

Distribuicao por frente:
- Frontend:
  - implementar fluxo inicial de login/cadastro.
  - consolidar telas de admin e chat no Figma.
- Backend:
  - endpoint de auth e validacao de token.
  - estrutura inicial de modulos e padrao de API.
- AI:
  - PoC de embedding/retrieval.
  - comparativo de opcoes (OpenSearch/pgvector para vetor).
- Documentacao:
  - ADRs iniciais e convencoes do projeto.
  - guia de onboarding do time.

Checklist de pronto da Sprint 0:
- login/cadastro funcionando em fluxo basico.
- templates de API e padrao de erro definidos.
- decisao inicial de armazenamento vetorial registrada.

## 4) Sprint 1 - Conteudo e taxonomia

Meta:
- Habilitar cadastro/publicacao de conteudo e tags com governanca minima.

Entregas:
- Backend:
  - CRUD de conteudo.
  - CRUD/listagem de tags.
  - status `DRAFT` e `PUBLISHED`.
- Frontend:
  - telas de lista/edicao/publicacao de conteudo.
  - associacao de tags e tipo de conteudo.
- AI:
  - contrato de metadados para ingestao.
- Documentacao:
  - contratos API atualizados.
  - fluxo funcional da jornada admin.

Checklist:
- conteudo publicado com tipo e ao menos 1 tag.
- auditoria minima de criacao/edicao/publicacao.

## 5) Sprint 2 - Ingestao AWS (S3 + Step Functions)

Meta:
- Colocar pipeline de ingestao assinado por estado.

Entregas:
- Backend:
  - upload e referencia de documentos no S3.
  - disparo de `StartIngestion` por publicacao/reindex.
  - endpoint de status de job.
- AI:
  - Lambdas/Python para extracao, chunking e embeddings.
  - gravacao de artefatos processados em S3.
- Infra:
  - Step Functions com retry/catch.
  - DynamoDB para estado de jobs e metadados.
- Documentacao:
  - runbook de reprocessamento.

Checklist:
- job percorre estados `PENDING/RUNNING/DONE/FAILED`.
- falha controlada com mensagem de erro rastreavel.

## 6) Sprint 3 - OpenSearch vetorial + Chat RAG

Meta:
- Entregar retrieval semantico e chat com fontes.

Entregas:
- AI:
  - indice vetorial no OpenSearch.
  - consulta `knn` com filtros por tag/tipo/status.
- Backend:
  - gateway WebSocket do chat para AI service.
  - persistencia de sessao/mensagens no DynamoDB.
- Frontend:
  - chat funcional com exibicao de citacoes.
- Documentacao:
  - guia de tuning de `topK` e filtros.

Checklist:
- resposta do chat retorna `answer` e `sources[]`.
- somente conteudo `PUBLISHED` entra no contexto.

## 7) Sprint 4 - MCP + observabilidade

Meta:
- Incorporar ferramentas MCP internas e observabilidade fim-a-fim.

Entregas:
- AI:
  - tools MCP internas (`search_published_content`, `get_document_snippet`).
  - guardrails de uso de ferramenta.
- Backend:
  - autorizacao e auditoria de chamadas sensiveis.
- Infra:
  - metricas, logs e traces (Datadog/CloudWatch).
- Documentacao:
  - playbook de incidentes e seguranca operacional.

Checklist:
- rastreabilidade de chamada de tool MCP.
- painis de observabilidade para API, ingestao e chat.

## 8) RACI simplificado por frente

Legenda:
- R = executa
- A = aprova
- C = consulta
- I = informado

Matriz resumida:
1. Auth e RBAC
- Backend: R/A
- Frontend: R/C
- AI: I
- Documentacao: C/I

2. Conteudo e tags
- Backend: R/A
- Frontend: R/C
- AI: C
- Documentacao: C/I

3. Ingestao
- AI: R
- Backend: R/C
- Infra: A
- Documentacao: C/I

4. Chat e RAG
- AI: R/A
- Backend: R/C
- Frontend: R/C
- Documentacao: I

5. Operacao e observabilidade
- Infra: R/A
- Backend: C
- AI: C
- Documentacao: R/C
