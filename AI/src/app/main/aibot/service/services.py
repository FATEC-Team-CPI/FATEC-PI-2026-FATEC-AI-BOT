import os
import shutil
from docling.document_converter import DocumentConverter
from langchain_text_splitters import RecursiveCharacterTextSplitter
from app.config import db

converter = DocumentConverter()
text_splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=100)

def process_and_store_document(file):
    temp_path = f"temp_{file.filename}"
    
    # Salva temporário
    with open(temp_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    # Extração Docling
    result = converter.convert(temp_path)
    text = result.document.export_to_markdown()

    #tratando arquivo via docling
    # from docling.document_converter import DocumentConverter                    
    # source = "https://arxiv.org/pdf/2408.09869"
    # converter = DocumentConverter()
    # doc = converter.convert(source).document
    # print(doc.export_to_markdown())

    # Chunking
    chunks = text_splitter.split_text(text)

    # Salva no Banco vetorial
    db.add_texts(texts=chunks, metadatas=[{"source": file.filename}] * len(chunks))
    
    os.remove(temp_path)
    return len(chunks)

def get_relevant_context(query: str, k=3):
    docs = db.similarity_search(query, k=k)
    context = "\n\n".join([doc.page_content for doc in docs])
    sources = [doc.metadata for doc in docs]
    return context, sources