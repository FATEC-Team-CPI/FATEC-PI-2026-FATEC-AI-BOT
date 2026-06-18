import os
import shutil
import json
import re
import unicodedata
import zipfile
import xml.etree.ElementTree as ET
from app.main.aibot.config import dynamo_table, s3_client, BUCKET_NAME
from datetime import datetime

def _chunk_text(text: str, chunk_size: int = 500, overlap: int = 50) -> list[str]:
    if chunk_size <= 0:
        return [text]
    if overlap >= chunk_size:
        overlap = max(0, chunk_size // 10)

    chunks: list[str] = []
    i = 0
    n = len(text)
    step = max(1, chunk_size - overlap)
    while i < n:
        chunks.append(text[i : i + chunk_size])
        i += step
    return [c for c in chunks if c.strip()]


def _normalize_document_text(text: str) -> str:
    normalized = unicodedata.normalize("NFKC", text or "")
    normalized = normalized.replace("\r\n", "\n").replace("\r", "\n")
    normalized = re.sub(r"[\u200b-\u200f\ufeff]", "", normalized)
    normalized = re.sub(r"[ \t]{2,}", " ", normalized)
    normalized = re.sub(r"\n{3,}", "\n\n", normalized)
    return normalized.strip()


def _build_structured_summary(text: str, max_items: int = 80) -> str:
    lines = [line.strip() for line in (text or "").splitlines() if line.strip()]
    summary_lines: list[str] = []
    seen: set[str] = set()

    date_pattern = re.compile(
        r"(\b\d{1,2}/\d{1,2}(?:/\d{2,4})?\b|\b\d{1,2}\s+de\s+[a-zçãéêíóú]+\s+de\s+\d{4}\b)",
        re.IGNORECASE,
    )
    month_pattern = re.compile(
        r"\b(janeiro|fevereiro|marco|março|abril|maio|junho|julho|agosto|setembro|outubro|novembro|dezembro)\b",
        re.IGNORECASE,
    )
    event_pattern = re.compile(
        r"\b(aula|prova|avaliacao|avaliação|recesso|feria|férias|feriado|matricula|matrícula|inscricao|inscrição|prazo|calendario|calendário)\b",
        re.IGNORECASE,
    )

    def add_line(label: str, value: str) -> None:
        normalized_value = value.strip()
        if not normalized_value:
            return
        key = f"{label}:{normalized_value}"
        if key in seen:
            return
        seen.add(key)
        summary_lines.append(f"- {label}: {normalized_value}")

    title = lines[0] if lines else "Documento processado"
    add_line("Título", title)

    for line in lines:
        if len(summary_lines) >= max_items:
            break

        if line.startswith("#"):
            heading = line.lstrip("#").strip()
            if heading:
                add_line("Seção", heading)
            continue

        if date_pattern.search(line) or month_pattern.search(line) or event_pattern.search(line):
            cleaned = re.sub(r"\s+", " ", line)
            if len(cleaned) > 220:
                cleaned = cleaned[:217].rstrip() + "..."
            add_line("Item", cleaned)

    if len(summary_lines) == 1:
        for line in lines[1: min(len(lines), 25)]:
            if len(summary_lines) >= max_items:
                break
            cleaned = re.sub(r"\s+", " ", line)
            if len(cleaned) > 220:
                cleaned = cleaned[:217].rstrip() + "..."
            add_line("Item", cleaned)

    return "\n".join(["# Resumo Estruturado", *summary_lines])

def _remove_images_and_large_tables(markdown: str) -> str:
    # Remove markdown image syntax ![alt](url) and HTML <img> tags and data URIs
    md = re.sub(r"!\[[^\]]*\]\([^\)\s]+\)", "", markdown)
    md = re.sub(r"<img[\s\S]*?>", "", md, flags=re.IGNORECASE)
    # Remove base64/data URIs
    md = re.sub(r"data:image\/[a-zA-Z0-9.+-]+;base64,[A-Za-z0-9+/=\n\r]+", "", md)

    # Heuristic: remove very wide tables (many pipes per line) or giant table blocks
    lines = md.splitlines()
    out_lines = []
    in_table = False
    table_buffer = []
    for line in lines:
        if re.match(r"^\s*\|", line) or ("|" in line and line.count("|") > 4):
            table_buffer.append(line)
            in_table = True
            # skip adding now
            continue
        else:
            if in_table:
                # decide whether to keep small tables
                table_text = "\n".join(table_buffer)
                if len(table_text) < 400 and table_text.count("\n") < 8:
                    out_lines.append(table_text)
                # reset
                table_buffer = []
                in_table = False
            out_lines.append(line)

    # flush
    if table_buffer:
        table_text = "\n".join(table_buffer)
        if len(table_text) < 400 and table_text.count("\n") < 8:
            out_lines.append(table_text)

    cleaned = "\n".join(out_lines)
    # Remove long URLs
    cleaned = re.sub(r"https?://\S{120,}", "", cleaned)
    # Collapse multiple blank lines
    cleaned = re.sub(r"\n{3,}", "\n\n", cleaned)
    return cleaned.strip()


def _chunk_markdown_by_section(markdown: str, max_chunk_chars: int = 1000, overlap: int = 100) -> list[str]:
    # Split by headings first
    lines = markdown.splitlines()
    sections = []
    current = []
    for line in lines:
        if line.strip().startswith("#") and current:
            sections.append("\n".join(current).strip())
            current = [line]
        else:
            current.append(line)
    if current:
        sections.append("\n".join(current).strip())

    chunks: list[str] = []
    for sec in sections:
        sec = sec.strip()
        if not sec:
            continue
        if len(sec) <= max_chunk_chars:
            chunks.append(sec)
            continue

        # fallback: break section into paragraphs and window-slide
        paras = [p.strip() for p in re.split(r"\n\s*\n", sec) if p.strip()]
        window = ""
        for p in paras:
            if not window:
                window = p
                continue
            if len(window) + 1 + len(p) <= max_chunk_chars:
                window = window + "\n\n" + p
                continue
            # emit window, create overlap
            chunks.append(window)
            # build overlap
            overlap_text = window[-overlap:] if overlap > 0 else ""
            window = (overlap_text + "\n\n" + p).strip()
        if window:
            # may still be large; hard cut
            if len(window) <= max_chunk_chars:
                chunks.append(window)
            else:
                # hard split
                i = 0
                step = max_chunk_chars - overlap
                while i < len(window):
                    part = window[i : i + max_chunk_chars]
                    chunks.append(part)
                    i += step

    # Final trim: remove empty and very short tokens
    cleaned_chunks = [c.strip() for c in chunks if c and len(c.strip()) > 30]
    return cleaned_chunks


def _extract_docx_text(path: str) -> str:
    # Fallback for .docx when Docling is unavailable.
    # Reads the OOXML document.xml directly so we never keep the raw ZIP bytes.
    namespaces = {
        "w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main",
        "m": "http://schemas.openxmlformats.org/officeDocument/2006/math",
    }

    try:
        with zipfile.ZipFile(path) as archive:
            with archive.open("word/document.xml") as xml_file:
                root = ET.parse(xml_file).getroot()

        paragraphs: list[str] = []
        for paragraph in root.findall(".//w:p", namespaces):
            pieces: list[str] = []
            for node in paragraph.iter():
                tag = node.tag
                if tag.endswith("}t") and node.text:
                    pieces.append(node.text)
                elif tag.endswith("}tab"):
                    pieces.append("\t")
            paragraph_text = "".join(pieces).strip()
            if paragraph_text:
                paragraphs.append(paragraph_text)

        if paragraphs:
            return "\n".join(paragraphs)
    except Exception:
        pass

    # Last resort: decode as text, but only after we failed to extract OOXML.
    with open(path, "rb") as f:
        return f.read().decode("utf-8", errors="ignore")


def _extract_pdf_text(path: str) -> str:
    # Fallback for PDFs when Docling is unavailable.
    # Uses pypdf to read embedded text; scanned PDFs still need OCR upstream.
    try:
        from pypdf import PdfReader
    except Exception:
        with open(path, "rb") as f:
            return f.read().decode("utf-8", errors="ignore")

    try:
        reader = PdfReader(path)
        pages: list[str] = []
        for page_number, page in enumerate(reader.pages, start=1):
            page_text = page.extract_text() or ""
            page_text = page_text.strip()
            if page_text:
                pages.append(f"# Página {page_number}\n{page_text}")

        if pages:
            return "\n\n".join(pages)
    except Exception:
        pass

    with open(path, "rb") as f:
        return f.read().decode("utf-8", errors="ignore")

try:
    from docling.document_converter import DocumentConverter
    converter = DocumentConverter()
except Exception:
    converter = None

def process_and_store_document(file):
    temp_path = f"temp_{file.filename}"

    # Salva temporário
    with open(temp_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    # Extração de texto: usa Docling quando disponível, senão faz fallback simples
    markdown_text = None
    if converter is not None:
        try:
            result = converter.convert(temp_path)
            markdown_text = result.document.export_to_markdown()
        except Exception:
            markdown_text = None

    if markdown_text is None:
        ext = os.path.splitext(file.filename or "")[1].lower()
        if ext == ".docx":
            markdown_text = _extract_docx_text(temp_path)
        elif ext == ".pdf":
            markdown_text = _extract_pdf_text(temp_path)
        else:
            with open(temp_path, "rb") as f:
                markdown_text = f.read().decode("utf-8", errors="ignore")

    # Limpeza inicial: remover imagens/data-uris e tabelas grandes
    cleaned_markdown = _remove_images_and_large_tables(markdown_text)

    # Normalizar para texto legível para sumarização
    cleaned_text = _normalize_document_text(cleaned_markdown)
    structured_summary = _build_structured_summary(cleaned_markdown)

    #tratando arquivo via docling
    # from docling.document_converter import DocumentConverter
    # source = "https://arxiv.org/pdf/2408.09869"
    # converter = DocumentConverter()
    # doc = converter.convert(source).document
    # print(doc.export_to_markdown())

    # Chunking (por seções de Markdown, com fallback)
    chunks = _chunk_markdown_by_section(cleaned_markdown, max_chunk_chars=1000, overlap=100)

    # Persistência no S3 (arquivo original + chunks)
    now_iso = datetime.utcnow().isoformat() + "Z"
    safe_name = file.filename.replace("\\", "_").replace("/", "_")
    original_key = f"documents/original/{safe_name}"
    markdown_key = f"documents/markdown/{safe_name}.md"
    summary_key = f"documents/summary/{safe_name}.md"
    chunks_key = f"documents/chunks/{safe_name}.jsonl"

    try:
        if s3_client is not None:
            with open(temp_path, "rb") as f:
                s3_client.put_object(
                    Bucket=BUCKET_NAME,
                    Key=original_key,
                    Body=f.read(),
                )

            s3_client.put_object(
                Bucket=BUCKET_NAME,
                Key=markdown_key,
                Body=cleaned_text.encode("utf-8"),
                ContentType="text/markdown; charset=utf-8",
            )

            s3_client.put_object(
                Bucket=BUCKET_NAME,
                Key=summary_key,
                Body=structured_summary.encode("utf-8"),
                ContentType="text/markdown; charset=utf-8",
            )

            jsonl = "".join(
                json.dumps({"i": idx, "text": chunk}, ensure_ascii=False) + "\n"
                for idx, chunk in enumerate(chunks)
            )
            s3_client.put_object(
                Bucket=BUCKET_NAME,
                Key=chunks_key,
                Body=jsonl.encode("utf-8"),
                ContentType="application/x-ndjson; charset=utf-8",
            )
    except Exception:
        # Non-fatal in MVP: we still try to store metadata; caller can inspect logs
        pass

    os.remove(temp_path)
    # Atualiza DynamoDB com metadados do documento (se estiver disponível)
    try:
        if dynamo_table is not None:
            item_doc = {
                "pk": "FatecItaquera#Conteudos",
                "sk": file.filename,
                "entityType": "CONTENT",
                "s3Key": original_key,
                "s3MarkdownKey": markdown_key,
                "s3SummaryKey": summary_key,
                "s3ChunksKey": chunks_key,
                "bucket": BUCKET_NAME,
                "status": "COMPLETED",
                "updated_at": now_iso,
                "created_at": now_iso,
                "gsi2pk": "UNIT#FatecItaquera#CONTENT",
                "gsi2sk": f"STATUS#COMPLETED#TS#{now_iso}#{file.filename}",
            }
            dynamo_table.put_item(Item=item_doc)
    except Exception:
        # Non-fatal: ingestion succeeded but Dynamo update failed
        pass

    return len(chunks)
