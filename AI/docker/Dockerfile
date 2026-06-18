# syntax=docker/dockerfile:1.7
FROM python:3.11-slim-bookworm

ENV LANG=C.UTF-8 \
  LC_ALL=C.UTF-8

RUN apt-get update && \
  apt-get install -y --no-install-recommends \
  build-essential \
  curl \
  gcc \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

COPY requirements.txt ./

RUN --mount=type=cache,target=/root/.cache/pip \
  pip install --upgrade pip && \
  pip install -r requirements.txt

COPY . /workspace

ENV PYTHONUNBUFFERED=1

EXPOSE 8002

CMD ["uvicorn", "app.main.aibot.model.main:app", "--host", "0.0.0.0", "--port", "8002", "--app-dir", "src"]
