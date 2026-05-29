import shutil
import os
from fastapi import FastAPI, UploadFile, File
import app.main.aibot.service.services as services

app = FastAPI(title="FATEC AI - Ingestion API")

@app.get("/")
def home():
    return {"status": "ok"}

@app.post("/upload")
async def upload_document(file: UploadFile = File(...)):
    total_chunks = services.process_and_store_document(file)
    return {"message": f"Arquivo {file.filename} processado.", "chunks": total_chunks}

@app.get("/sync-docs")
async def syncDocsFromDB():
    # Placeholder: in a full implementation this would fetch files from backend/S3 or DB
    return {"status": "not_implemented"}
