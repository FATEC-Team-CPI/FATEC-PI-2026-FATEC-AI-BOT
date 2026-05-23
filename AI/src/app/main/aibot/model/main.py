import shutil
import os
from fastapi import FastAPI, UploadFile, File
from app.config import client, db, CHROMA_PATH, embeddings
import app.services

app = FastAPI(title="IABotAgent FATEC API")

# deve ser possivel mudar pelo adm
behaivor_ia = """
Você é o assistente virtual oficial do projeto IABotAgent da FATEC.
Sua função é responder perguntas de forma clara, técnica e prestativa.
Sempre utilize as informações da FATEC para embasar suas respostas.
Se não souber algo, recomende pesquisar nos sites oficiais da FATEC.
"""

@app.get("/")
def home():
    return {"status": "ok"}

# @app.post("/upload")
# async def upload_document(file: UploadFile = File(...)):
#     total_chunks = services.process_and_store_document(file)
#     return {"message": f"Arquivo {file.filename} processado.", "chunks": total_chunks}

@app.get("/sync-docs")
async def syncDocsFromDB(doc: UploadFile):

   #pegar docs do banco de dados
   # front chama api de enviar arquivo pro backend
   # backend envia pro banco 
   # api pega do banco 
   # trata o arquivo via docling aqui 
   
   return {"nome": doc.filename, "tipo": doc.content_type}

@app.get("/ask")
def askFromIA(pergunta: str):
    contexto, fontes = services.get_relevant_context(pergunta)
    
    messages = [
        {
            "role": "system", 
            "content": (
                f"Comportamento: {behaivor_ia}\n\n"
                f"Contexto para respostas: {contexto}"
            )
        },
        {"role": "user", "content": pergunta}
    ]
    
    response = client.chat.completions.create(
        model="Qwen/Qwen2.5-72B-Instruct",
        messages=messages,
        max_tokens=500
    )
    return {"resposta": response.choices[0].message.content, "fontes": fontes}


@app.get("/listar-documentos")
def listar():
    dados = db.get()
    arquivos = list({m['source'] for m in dados['metadatas']}) if dados['metadatas'] else []
    return {"documentos": arquivos}

