# Proposta de Taxonomia e Tags de Conteudo

## 1) Objetivo
Padronizar classificacao de conteudo para:
- Melhorar busca semantica (RAG) no chat.
- Permitir filtros no admin e no chat.
- Aumentar governanca e auditoria do acervo da Fatec.

## 2) Principios
- Ter um tipo principal de conteudo (1 por item).
- Permitir multiplas tags de assunto por conteudo.
- Evitar tags livres no MVP para reduzir ruido.
- Manter dicionario controlado de tags com sinimos.

## 3) Modelo de classificacao

### 3.1 Tipo principal (`content_type`)
Sugestao inicial:
- `PAGE` (pagina institucional)
- `DOCUMENT` (PDF, DOCX, regulamentos)
- `NOTICE` (avisos/comunicados)
- `FAQ` (perguntas frequentes)
- `REGULATION` (normas/editais/regimentos)

Regra:
- Campo obrigatorio no cadastro do conteudo.

### 3.2 Tags de assunto (`tag`)
Sugestao inicial de assuntos:
- `vestibular`
- `matricula`
- `estagio`
- `calendario-academico`
- `bolsas`
- `secretaria`
- `tcc`
- `atendimento`
- `financeiro`
- `grade-curricular`

Regra:
- Cada conteudo deve ter entre 1 e 5 tags de assunto no MVP.

### 3.3 Dimensoes opcionais (fase 2)
- `campus` (unidade)
- `curso`
- `publico-alvo` (`aluno`, `candidato`, `docente`)
- `periodo` (semestre/ano)

## 4) Modelo de dados sugerido (SQL)

```sql
create table content (
  id uuid primary key,
  title varchar(255) not null,
  content_type varchar(32) not null,
  status varchar(16) not null,
  created_at timestamp not null,
  updated_at timestamp not null
);

create table tag (
  id uuid primary key,
  slug varchar(80) not null unique,
  name varchar(120) not null,
  category varchar(40) not null,
  is_active boolean not null default true,
  created_at timestamp not null
);

create table content_tag (
  content_id uuid not null,
  tag_id uuid not null,
  created_at timestamp not null,
  primary key (content_id, tag_id),
  constraint fk_content_tag_content foreign key (content_id) references content(id),
  constraint fk_content_tag_tag foreign key (tag_id) references tag(id)
);

create index idx_content_type on content(content_type);
create index idx_tag_category on tag(category);
create index idx_content_tag_tag on content_tag(tag_id);
```

Observacao:
- Em `tag.category`, usar no minimo `SUBJECT` no MVP.

## 5) API minima para tags

### Admin
- `GET /tags?category=SUBJECT`
- `POST /tags` (somente SUPER_ADMIN)
- `PATCH /tags/{id}` (ativar/desativar, renomear)

### Conteudo
- `POST /contents` com `contentType` e `tagIds[]`
- `PUT /contents/{id}` para atualizar `contentType` e `tagIds[]`

Payload exemplo:

```json
{
  "title": "Calendario Academico 2026",
  "contentType": "DOCUMENT",
  "tagIds": [
    "c1d8a8b3-4fbe-42dd-9a5f-2b3c63cd45d4",
    "e346c5f2-50eb-4af8-a0fa-a7a8a58414f8"
  ]
}
```

## 6) Uso no pipeline de ingestao
- Persistir metadados de classificacao em cada chunk:
  - `content_type`
  - `tag_slugs[]`
  - `content_id`
  - `source_title`
- Ao reindexar, apagar chunks antigos e recriar com metadados atualizados.
- Se tag mudar, disparar reindexacao do conteudo.

Metadado exemplo por chunk:

```json
{
  "content_id": "4d4e8cb0-2f8a-4f6c-95c0-2c6ffdc8a905",
  "content_type": "DOCUMENT",
  "tag_slugs": ["calendario-academico", "matricula"],
  "source_title": "Calendario Academico 2026"
}
```

## 7) Uso no chat (retrieval)

### 7.1 Sem filtro explicito (padrao)
- Busca vetorial geral no acervo `PUBLISHED`.
- Re-ranking favorece chunks com tags mais proximas da pergunta.

### 7.2 Com filtro explicito (quando detectado)
- Se pergunta mencionar tema claro (ex.: "estagio"), aplicar filtro por `tag_slugs`.
- Se mencionar tipo (ex.: "edital"), priorizar `content_type=REGULATION`.

### 7.3 Regras de seguranca
- Nunca recuperar conteudo `DRAFT`.
- Exigir ao menos 1 fonte na resposta quando houver contexto.
- Sem contexto suficiente: responder fallback seguro e orientar consulta a canais oficiais.

## 8) Governanca da taxonomia
- Dicionario inicial mantido por SUPER_ADMIN.
- Proibir duplicidade semantica (`matricula` vs `matriculas`).
- Usar `slug` estavel e `name` editavel.
- Desativar tags antigas (`is_active=false`) em vez de apagar historico.

## 9) Estrategia de rollout
1. Criar migracoes de `tag` e `content_tag`.
2. Seed inicial de tags de assunto.
3. Ajustar CRUD de conteudo para exigir `content_type` e `tagIds`.
4. Atualizar ingestao para salvar metadados por chunk.
5. Atualizar retrieval para usar filtros por tag/tipo.

## 10) Criterios de pronto
- Todo conteudo publicado possui `content_type` e pelo menos 1 tag.
- Busca no chat retorna fontes com metadados corretos.
- Filtro por assunto melhora relevancia percebida nas respostas.
