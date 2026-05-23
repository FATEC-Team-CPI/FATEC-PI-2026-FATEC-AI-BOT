import os
from huggingface_hub import InferenceClient
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_community.vectorstores import Chroma

# Configurações de Caminho e Token
CHROMA_PATH = "./chroma_db"
HF_TOKEN = os.getenv("HF_TOKEN")

# Inicialização da IA
client = InferenceClient(api_key=HF_TOKEN)

# Inicialização dos Embeddings (Modelo leve para CPU)
embeddings = HuggingFaceEmbeddings(model_name="sentence-transformers/all-MiniLM-L6-v2")

# Inicialização do Banco Vetorial
db = Chroma(persist_directory=CHROMA_PATH, embedding_function=embeddings)