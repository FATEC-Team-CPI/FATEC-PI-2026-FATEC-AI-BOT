"""
MCP Server para FATEC AI Bot
Expõe tools de busca em documentos institucionais via HTTP/SSE
"""

import asyncio
import logging
from datetime import datetime
from collections import deque
from html.parser import HTMLParser
import re
import unicodedata
from fastmcp import FastMCP
import os
from urllib.parse import urljoin, urlparse

import httpx
from boto3.dynamodb.conditions import Attr, Key

# Configuração de logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("fatec-mcp")

# Inicializar servidor MCP
mcp = FastMCP("fatec-docs")

OFFICIAL_SITE_URL = os.getenv("FATEC_OFFICIAL_SITE_URL", "https://fatecitaquera.cps.sp.gov.br/")
OFFICIAL_SITE_DOMAIN = urlparse(OFFICIAL_SITE_URL).netloc.lower()


class _SitePageParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.title = ""
        self.meta_description = ""
        self.links: list[str] = []
        self._text_parts: list[str] = []
        self._capture_title = False
        self._ignore_depth = 0
        self._capture_text = True
        self._current_tag = ""

    def handle_starttag(self, tag, attrs):
        attrs_dict = dict(attrs)
        if tag in {"script", "style", "noscript"}:
            self._ignore_depth += 1
            return

        self._current_tag = tag

        if tag == "title":
            self._capture_title = True

        if tag == "meta" and attrs_dict.get("name", "").lower() == "description":
            self.meta_description = attrs_dict.get("content", "").strip()

        if tag == "a":
            href = attrs_dict.get("href", "").strip()
            if href:
                self.links.append(href)

    def handle_endtag(self, tag):
        if tag in {"script", "style", "noscript"} and self._ignore_depth > 0:
            self._ignore_depth -= 1
            return

        if tag == "title":
            self._capture_title = False

        if tag in {"p", "div", "li", "section", "article", "br", "h1", "h2", "h3", "h4"}:
            self._text_parts.append("\n")

    def handle_data(self, data):
        if self._ignore_depth > 0:
            return

        text = data.strip()
        if not text:
            return

        if self._capture_title:
            self.title += f" {text}".strip()
        else:
            self._text_parts.append(text)

    def get_text(self) -> str:
        text = " ".join(self._text_parts)
        text = re.sub(r"\s+", " ", text)
        return text.strip()


def _is_same_domain(url: str) -> bool:
    parsed = urlparse(url)
    return parsed.netloc.lower() == OFFICIAL_SITE_DOMAIN or parsed.netloc == ""


def _clean_url(url: str) -> str:
    parsed = urlparse(url)
    return parsed._replace(fragment="").geturl()


def _split_sentences(text: str) -> list[str]:
    parts = re.split(r"(?<=[.!?])\s+|\n+", text or "")
    return [part.strip() for part in parts if part.strip()]


def _find_relevant_sentences(text: str, query_tokens: list[str], limit: int = 5) -> list[str]:
    sentences = _split_sentences(text)
    if not sentences:
        return []

    if not query_tokens:
        return sentences[:limit]

    ranked: list[tuple[int, str]] = []
    for sentence in sentences:
        sentence_norm = unicodedata.normalize("NFKD", sentence)
        sentence_norm = "".join(char for char in sentence_norm if not unicodedata.combining(char))
        sentence_norm = re.sub(r"[^a-z0-9]+", " ", sentence_norm.lower())
        score = sum(1 for token in query_tokens if token in sentence_norm)
        if score > 0:
            ranked.append((score, sentence))

    ranked.sort(key=lambda item: item[0], reverse=True)
    return [sentence for _, sentence in ranked[:limit]]


def _normalize_lookup(value: str) -> str:
    return re.sub(r"\s+", " ", unicodedata.normalize("NFKD", value or "").encode("ascii", "ignore").decode("ascii").lower()).strip()


async def _fetch_page(client: httpx.AsyncClient, url: str) -> tuple[dict | None, list[str]]:
    try:
        response = await client.get(url, follow_redirects=True)
        response.raise_for_status()
        parser = _SitePageParser()
        parser.feed(response.text)

        page_url = _clean_url(str(response.url))
        page_text = parser.get_text()
        links = []
        for link in parser.links:
            absolute_link = _clean_url(urljoin(page_url, link))
            if _is_same_domain(absolute_link):
                links.append(absolute_link)

        page_info = {
            "url": page_url,
            "title": parser.title.strip() or page_url,
            "description": parser.meta_description,
            "text": page_text,
        }
        return page_info, links
    except Exception as exc:
        logger.warning("Falha ao acessar %s: %s", url, exc)
        return None, []


async def _crawl_official_site(query: str, max_pages: int = 5) -> list[dict]:
    query_tokens = DocumentRepository()._tokenize(query)
    visited: set[str] = set()
    queue = deque([OFFICIAL_SITE_URL])
    results: list[dict] = []

    timeout = httpx.Timeout(12.0, connect=6.0)
    async with httpx.AsyncClient(timeout=timeout, headers={"User-Agent": "FatecMCP/1.0"}) as client:
        while queue and len(visited) < max_pages:
            current_url = queue.popleft()
            if current_url in visited:
                continue

            visited.add(current_url)
            page_info, links = await _fetch_page(client, current_url)
            if not page_info:
                continue

            relevant_sentences = _find_relevant_sentences(page_info["text"], query_tokens, limit=4)
            score = len(relevant_sentences)
            if not query_tokens:
                score = 1

            if relevant_sentences or not query_tokens:
                excerpt = " ".join(relevant_sentences).strip()
                if len(excerpt) > 900:
                    excerpt = excerpt[:897].rstrip() + "..."

                results.append(
                    {
                        "source": "Fatec Itaquera Official Site",
                        "url": page_info["url"],
                        "title": page_info["title"],
                        "description": page_info["description"],
                        "excerpt": excerpt,
                        "relevance": score,
                    }
                )

            for link in links:
                if link not in visited and len(visited) + len(queue) < max_pages * 4:
                    queue.append(link)

    results.sort(key=lambda item: item["relevance"], reverse=True)
    trimmed = []
    for item in results[:2]:
        excerpt = item.get("excerpt", "")
        if len(excerpt) > 500:
            excerpt = excerpt[:497].rstrip() + "..."
        trimmed.append({**item, "excerpt": excerpt})
    return trimmed




# ============================================================================
# SIMULAÇÃO DE REPOSITÓRIO DE DOCUMENTOS
# Em produção, integrar com banco de dados real
# ============================================================================

class DocumentRepository:
    """Simula repositório de documentos - substituir com banco real"""
    def __init__(self):
        # Try to use DynamoDB (LocalStack) if available, otherwise keep a fallback in-memory dataset
        self.dynamo_table = None
        self.s3 = None
        try:
            import boto3
            from botocore.config import Config

            ddb_endpoint = os.getenv('AWS_DYNAMODB_ENDPOINT_OVERRIDE')
            aws_region = os.getenv('AWS_DEFAULT_REGION', 'us-east-1')
            cfg = Config(retries={'max_attempts': 2})
            client_kwargs = {
                'region_name': aws_region,
                'aws_access_key_id': os.getenv('AWS_ACCESS_KEY_ID', 'test'),
                'aws_secret_access_key': os.getenv('AWS_SECRET_ACCESS_KEY', 'test'),
                'config': cfg,
            }

            if ddb_endpoint:
                client_kwargs['endpoint_url'] = ddb_endpoint

            resource = boto3.resource(
                'dynamodb',
                **client_kwargs,
            )
            table_name = os.getenv('DDB_TABLE_NAME', 'fatec-ai-bot-core')
            self.dynamo_table = resource.Table(table_name)

            # S3 client (AWS or LocalStack, depending on configuration)
            self.bucket = os.getenv("BUCKET_NAME", "fatec-ai-bot-bucket")
            s3_endpoint = os.getenv("AWS_S3_ENDPOINT_OVERRIDE")
            s3_client_kwargs = dict(client_kwargs)
            if s3_endpoint:
                s3_client_kwargs['endpoint_url'] = s3_endpoint
            self.s3 = boto3.client(
                "s3",
                **s3_client_kwargs,
            )
        except Exception:
            self.dynamo_table = None
            self.s3 = None

        # Fallback in-memory documents for environments without DynamoDB
        self.documents = {
            "calendario_academico": {
                "id": "doc_001",
                "name": "Calendário Acadêmico 2025",
                "type": "calendario_academico",
                "description": "Datas de aulas, provas, recessos e férias",
                "updated_at": datetime.now().isoformat(),
                "content": [
                    {"text": "Aulas iniciam em 3 de março de 2025", "relevance": 0.95},
                ]
            },
        }

    def _normalize_text(self, value: str) -> str:
        normalized = unicodedata.normalize("NFKD", value or "")
        without_accents = "".join(char for char in normalized if not unicodedata.combining(char))
        return re.sub(r"[^a-z0-9]+", " ", without_accents.lower()).strip()

    def _tokenize(self, value: str) -> list[str]:
        stopwords = {
            "a", "as", "o", "os", "e", "de", "da", "das", "do", "dos", "em", "na",
            "no", "nas", "nos", "para", "por", "com", "um", "uma", "que", "vc", "voce",
            "voces", "sobre", "me", "meu", "minha", "quer", "quero", "gostaria", "saber",
            "informacao", "informacoes", "detalhes", "traga", "tem", "vc", "ta"
        }
        aliases = {
            "inicio": "inicio",
            "inicia": "inicio",
            "iniciam": "inicio",
            "comeca": "inicio",
            "comeca": "inicio",
            "comecam": "inicio",
            "comeca": "inicio",
            "aula": "aula",
            "aulas": "aula",
            "semestre": "semestre",
            "calendario": "calendario",
        }
        tokens = []
        for token in self._normalize_text(value).split():
            if len(token) <= 2 or token in stopwords:
                continue
            tokens.append(aliases.get(token, token))
        return tokens

    def _score_chunk(self, query_tokens: list[str], chunk_text: str) -> float:
        if not query_tokens:
            return 0.0

        chunk_norm = self._normalize_text(chunk_text)
        if not chunk_norm:
            return 0.0

        chunk_tokens = set(chunk_norm.split())
        overlap = len(set(query_tokens) & chunk_tokens)
        token_score = overlap / max(1, len(set(query_tokens)))

        phrase_bonus = 0.0
        query_phrase = " ".join(query_tokens)
        if query_phrase and query_phrase in chunk_norm:
            phrase_bonus = 0.75

        semantic_bonus = 0.0
        if "inicio" in query_tokens and ("inicio" in chunk_tokens or "inicia" in chunk_norm or "iniciam" in chunk_norm):
            semantic_bonus += 0.5
        if "aula" in query_tokens and ("aula" in chunk_tokens or "aulas" in chunk_norm):
            semantic_bonus += 0.4
        if "calendario" in query_tokens and "calendario" in chunk_tokens:
            semantic_bonus += 0.4

        return token_score + phrase_bonus + semantic_bonus

    def _is_broad_query(self, query: str) -> bool:
        broad_tokens = {"tudo", "mais", "detalhes", "completo", "completa", "documentacao", "documentação", "info", "informacao", "informações", "informacoes", "preciso", "precisa"}
        query_tokens = set(self._tokenize(query))
        return bool(query_tokens & broad_tokens) or len(query_tokens) <= 3

    async def _pick_best_document(self, query: str, document_type: str) -> dict | None:
        documents = await self.list_all()
        if not documents:
            return None

        lookup_type = self._normalize_text(document_type)
        query_tokens = set(self._tokenize(query))

        best_doc = None
        best_score = -1.0

        for doc in documents:
            doc_text = " ".join(
                str(doc.get(field, "")) for field in ("name", "description", "document_type", "id")
            )
            doc_norm = self._normalize_text(doc_text)
            doc_tokens = set(self._tokenize(doc_text))

            score = 0.0
            if lookup_type:
                if lookup_type == doc_norm:
                    score += 4.0
                if lookup_type in doc_norm:
                    score += 2.5
            score += len(query_tokens & doc_tokens)

            if {"calendario", "aula", "inicio"} & query_tokens and "calendario" in doc_norm:
                score += 3.0
            if {"documento", "documentos", "disponiveis"} & query_tokens:
                score += 0.1

            if score > best_score:
                best_score = score
                best_doc = doc

        return best_doc
    async def list_all(self):
        """Lista todos os documentos disponíveis"""
        # If DynamoDB is configured, query for CONTENT items with status COMPLETED
        if self.dynamo_table is not None:
            try:
                await asyncio.sleep(0.05)
                # Scan for CONTENT items with status COMPLETED (small dataset in local dev)
                response = self.dynamo_table.scan(
                    FilterExpression=Attr('entityType').eq('CONTENT') & Attr('status').eq('COMPLETED')
                )
                items = response.get('Items', [])
                return [
                    {
                        'id': item.get('sk'),
                        'name': item.get('sk'),
                        'type': item.get('entityType', 'CONTENT'),
                        'document_type': item.get('sk'),
                        'description': item.get('s3Key', ''),
                        'summary_key': item.get('s3SummaryKey', ''),
                        'chunks_key': item.get('s3ChunksKey', ''),
                        'bucket': item.get('bucket', self.bucket if hasattr(self, "bucket") else ''),
                        'updated_at': item.get('updated_at'),
                    }
                    for item in items
                ]
            except Exception:
                # fallback to in-memory
                pass

        await asyncio.sleep(0.1)
        return [
            {
                "id": doc["id"],
                "name": doc["name"],
                "type": doc["type"],
                "document_type": doc["type"],
                "description": doc["description"],
                "updated_at": doc["updated_at"],
            }
            for doc in self.documents.values()
        ]

    async def search(self, query: str, document_type: str, limit: int = 1):
        """Busca conteúdo dentro de um documento específico"""
        await asyncio.sleep(0.15)  # Simula latência de busca

        if not query:
            query = document_type or ""

        lookup_type = self._normalize_text(document_type)
        generic_type = lookup_type in {"", "content", "document", "documents", "docs", "arquivo", "arquivos"}
        
        # Fetch chunks from S3 (no embeddings).
        if self.dynamo_table is not None and self.s3 is not None:
            try:
                meta = self.dynamo_table.get_item(
                    Key={"pk": "FatecItaquera#Conteudos", "sk": document_type}
                ).get("Item")
                if not meta:
                    selected_doc = await self._pick_best_document(query, document_type)
                    if selected_doc:
                        meta = self.dynamo_table.get_item(
                            Key={"pk": "FatecItaquera#Conteudos", "sk": selected_doc.get("document_type") or selected_doc.get("id") or selected_doc.get("name")}
                        ).get("Item")
                if not meta:
                    return []

                bucket = meta.get("bucket") or self.bucket
                summary_key = meta.get("s3SummaryKey")
                chunks_key = meta.get("s3ChunksKey")
                query_tokens = self._tokenize(query)
                scored_chunks = []

                if summary_key:
                    try:
                        summary_obj = self.s3.get_object(Bucket=bucket, Key=summary_key)
                        summary_text = summary_obj["Body"].read().decode("utf-8", errors="ignore")
                        if summary_text.strip():
                            scored_chunks.append((self._score_chunk(query_tokens, summary_text) + 1.0, summary_text))
                    except Exception:
                        pass

                if chunks_key:
                    obj = self.s3.get_object(Bucket=bucket, Key=chunks_key)
                    body = obj["Body"].read().decode("utf-8", errors="ignore").splitlines()

                    for line in body[:5000]:  # hard cap for local MVP
                        if not line.strip():
                            continue
                        try:
                            rec = __import__("json").loads(line)
                            content = rec.get("text", "")
                        except Exception:
                            content = line

                        score = self._score_chunk(query_tokens, content)
                        scored_chunks.append((score, content))

                if not scored_chunks:
                    return []

                scored_chunks.sort(key=lambda item: item[0], reverse=True)
                desired_limit = max(2, min(limit, 4)) if self._is_broad_query(query) else max(1, min(limit, 2))
                top_chunks = [item for item in scored_chunks if item[0] > 0][:desired_limit]

                # If the query is too broad, give a small representative slice of the document.
                if not top_chunks:
                    top_chunks = scored_chunks[:desired_limit]

                # Deduplicate very similar snippets so broad questions get coverage, not repetition.
                unique_chunks = []
                seen_snippets: set[str] = set()
                for score, content in top_chunks:
                    normalized_snippet = self._normalize_text(content[:220])
                    if not normalized_snippet or normalized_snippet in seen_snippets:
                        continue
                    seen_snippets.add(normalized_snippet)
                    unique_chunks.append((score, content))
                top_chunks = unique_chunks

                # Prefer a short bundle of the strongest material instead of a single fragment.
                if self._is_broad_query(query) and len(top_chunks) > 1:
                    top_chunks = top_chunks[:4]

                resolved_document_type = meta.get("sk") or document_type
                return [
                    {
                        "content": content,
                        "source": resolved_document_type,
                        "document_type": resolved_document_type,
                        "relevance": score,
                    }
                    for score, content in top_chunks
                ]
            except Exception:
                pass

        # Fallback: in-memory naive search
        if document_type not in self.documents:
            selected_doc = await self._pick_best_document(query, document_type)
            if not selected_doc:
                return []
            document_type = selected_doc.get("document_type") or selected_doc.get("type") or selected_doc.get("id") or document_type

        doc = self.documents[document_type]
        if not doc["content"]:
            return []

        first_item = doc["content"][0]
        return [
            {
                "content": first_item["text"],
                "source": doc["name"],
                "document_type": document_type,
                "relevance": first_item["relevance"],
            }
        ]


# Instanciar repositório
document_repository = DocumentRepository()


# ============================================================================
# TOOLS MCP
# ============================================================================

@mcp.tool()
async def list_available_documents() -> list[dict]:
    """
    Lista todos os documentos disponíveis na base de conhecimento
    da FATEC Itaquera. SEMPRE chame esta função primeiro, antes de
    buscar, para identificar qual documento é mais relevante.
    
    Retorna uma lista de documentos com seus metadados.
    """
    logger.info("📋 Listando documentos disponíveis...")
    documents = await document_repository.list_all()
    logger.info(f"✅ {len(documents)} documentos encontrados")
    return documents


@mcp.tool()
async def search_fatec_documents(
    query: str,
    document_type: str
) -> list[dict]:
    """
    Busca informações dentro de um documento da FATEC Itaquera.
    Só chame após usar list_available_documents() para identificar
    o document_type correto.
    
    Args:
        query: Pergunta ou termo de busca do usuário
        document_type: Tipo exato do documento (ex: calendario_academico, 
                      edital_vestibular, grade_curricular, regulamento, contato)
    
    Retorna uma lista de resultados com conteúdo, fonte e relevância.
    """
    logger.info(f"🔍 Buscando em '{document_type}': {query}")
    
    results = await document_repository.search(
        query=query,
        document_type=document_type,
        limit=1
    )
    
    if not results:
        logger.warning(f"⚠️ Nenhum resultado encontrado para: {query}")
    else:
        logger.info(f"✅ {len(results)} resultado(s) encontrado(s)")
    
    return results


@mcp.tool()
async def search_fatec_itaquera_site(query: str, max_pages: int = 5) -> list[dict]:
    """
    Busca informações no site oficial da FATEC Itaquera.

    Args:
        query: Termo ou pergunta de busca
        max_pages: Número máximo de páginas para varrer na navegação

    Retorna trechos relevantes do site oficial, com URL e relevância.
    """
    safe_max_pages = max(1, min(int(max_pages), 10))
    logger.info("🌐 Buscando no site oficial (%s páginas): %s", safe_max_pages, query)
    results = await _crawl_official_site(query=query, max_pages=safe_max_pages)

    if not results:
        logger.warning("⚠️ Nenhum resultado encontrado no site oficial para: %s", query)
    else:
        logger.info("✅ %s resultado(s) encontrado(s) no site oficial", len(results))

    return results


# ============================================================================
# HEALTH CHECK
# ============================================================================

@mcp.tool()
async def health_check() -> dict:
    """
    Verifica saúde do servidor MCP
    
    Retorna status e informações do servidor
    """
    return {
        "status": "healthy",
        "timestamp": datetime.now().isoformat(),
        "service": "fatec-mcp-server",
        "version": "1.0.0",
    }


# ============================================================================
# INICIALIZAÇÃO
# ============================================================================

if __name__ == "__main__":
    host = os.getenv("MCP_HOST", "0.0.0.0")
    port = int(os.getenv("MCP_PORT", "8001"))

    logger.info("🚀 Iniciando MCP Server FATEC...")
    logger.info("📡 Expondo tools via MCP SSE na porta %s", port)
    logger.info("   - list_available_documents()")
    logger.info("   - search_fatec_documents(query, document_type)")
    logger.info("   - search_fatec_itaquera_site(query, max_pages)")
    logger.info("   - health_check()")

    # Inicia o servidor MCP via SSE (legacy)
    mcp.run(transport="sse", host=host, port=port)
