# Arquitetura - MVP Enxuto e Evolucao AWS

## 1) Objetivo
Registrar o desenho arquitetural em 2 etapas:
- Entrega inicial simples (MVP).
- Evolucao gradual com servicos AWS (Lambda, DynamoDB, Step Functions).

## 2) Diagrama

```mermaid
flowchart TB
  subgraph A[MVP enxuto - foco em entrega rapida]
    U[Usuario web/mobile] --> WS[Chat WebSocket no Quarkus]
    ADM[Admin] --> API[API REST Quarkus]
    API --> PG[(PostgreSQL)]
    API --> FILES[(Storage local ou S3 compativel)]
    API --> JOBS[(Tabela ingestion_job)]
    JOBS --> WORKER[Worker Scheduler Quarkus]
    WORKER --> PARSE[Extracao + chunking]
    PARSE --> EMB[Embeddings]
    EMB --> VEC[(pgvector no Postgres)]
    WS --> RET[Retrieval RAG]
    RET --> VEC
    WS --> LLM[Provider LLM]
    RET --> LLM
    LLM --> WS
  end

  subgraph B[Evolucao AWS - escalabilidade gradual]
    U2[Usuario] --> APIGW[API Gateway]
    APIGW --> ECS[Quarkus em ECS/Fargate ou EC2]
    ADM2[Admin] --> APIGW

    ECS --> RDS[(RDS PostgreSQL)]
    ECS --> S3[(S3 documentos)]
    ECS --> SFN[Step Functions]

    SFN --> L1[Lambda extracao]
    SFN --> L2[Lambda chunking]
    SFN --> L3[Lambda embeddings]

    L3 --> VDB[(Vector store: pgvector ou OpenSearch)]
    ECS --> DDB[(DynamoDB sessao/cache chat)]

    ECS --> DD[Datadog APM + Logs + Metrics]
    L1 --> DD
    L2 --> DD
    L3 --> DD
  end

  DEV[Dev local] --> LS[LocalStack]
  LS --> S3
  LS --> SFN
  LS --> DDB

  classDef mvp fill:#E8F5E9,stroke:#2E7D32,stroke-width:1px;
  classDef aws fill:#E3F2FD,stroke:#1565C0,stroke-width:1px;
  class A mvp;
  class B aws;
```

## 3) Leitura do desenho

### 3.1 MVP enxuto
- Quarkus concentra API admin, chat e worker de ingestao.
- Fila simples por tabela `ingestion_job` no Postgres.
- Vetor store no mesmo banco com `pgvector`.
- Menor custo operacional para validar o produto.

### 3.2 Evolucao AWS
- Quarkus continua como servico central de negocio.
- Ingestao pesada vai para Step Functions + Lambdas.
- S3 armazena documentos e DynamoDB guarda sessao/cache de chat.
- Observabilidade no Datadog para backend e funcoes serverless.

### 3.3 LocalStack primeiro
- Em desenvolvimento: simular S3, Step Functions e DynamoDB.
- Em producao: migrar para AWS real no Free Tier com limites e alarmes de custo.

## 4) Quando migrar para AWS
- Aumentou volume de ingestao ou tempo medio por job.
- Worker interno comecou a competir com latencia da API.
- Necessidade de retries mais robustos e orquestracao por etapas.
- Necessidade de escalar partes especificas sem escalar tudo.

## 5) Ordem sugerida de adocao
1. S3 para armazenamento de documentos.
2. Step Functions para orquestrar pipeline.
3. Lambda para extracao/chunking/embedding.
4. DynamoDB para sessao/cache de chat.
5. Ajustes de custo, observabilidade e governanca.
