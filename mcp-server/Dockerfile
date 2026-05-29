FROM python:3.11-slim

WORKDIR /app

# Instalar dependências do sistema
RUN apt-get update && apt-get install -y \
    curl \
    git \
    && rm -rf /var/lib/apt/lists/*

# Copiar requirements
COPY requirements.txt .

# Instalar dependências Python
RUN pip install --no-cache-dir -r requirements.txt

# Copiar código
COPY mcp_server.py .

# Health check (porta aberta)
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD python -c "import socket; s=socket.create_connection(('localhost',8001),2); s.close()" || exit 1

# Expor porta
EXPOSE 8001

# Executar servidor
CMD ["python", "mcp_server.py"]
