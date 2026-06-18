import shutil
import os
from fastapi import FastAPI, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
import app.main.aibot.service.services as services

app = FastAPI(title="FATEC AI - Ingestion API")

# Allow the frontend origins used in development
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173", "http://127.0.0.1:5173"],
    allow_credentials=True,
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["*"],
)

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
