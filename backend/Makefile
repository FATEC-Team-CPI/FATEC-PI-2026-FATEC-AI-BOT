APP_NAME := fatec-ai-bot-backend

DEV_IMAGE := $(APP_NAME):dev
PROD_IMAGE := $(APP_NAME):prod

DEV_CONTAINER := $(APP_NAME)-dev
PROD_CONTAINER := $(APP_NAME)-prod
DOCKER_COMPOSE_FILE := docker-compose.yml
LOCALSTACK_COMPOSE_FILE := docker-compose.localstack.yml
LOCALSTACK_SERVICE := localstack

MAVEN_IMAGE := maven:3.9.9-eclipse-temurin-25
PROD_DOCKERFILE := Dockerfile.prod

# Windows shells (ex.: mingw32-make via PowerShell) may not expose PWD/HOME.
HOST_WORKSPACE := $(if $(PWD),$(PWD),$(CURDIR))
HOST_HOME := $(if $(HOME),$(HOME),$(USERPROFILE))
HOST_WORKSPACE := $(subst \\,/,$(HOST_WORKSPACE))
HOST_HOME := $(subst \\,/,$(HOST_HOME))
M2_DIR := $(HOST_HOME)/.m2

ifeq ($(OS),Windows_NT)
NULL_DEV := NUL
MVNW := .\\mvnw.cmd
else
NULL_DEV := /dev/null
MVNW := ./mvnw
endif

.PHONY: help generate-keys dev-image dev-up dev-shell dev-down build-artifacts prod-image prod-up prod-down prod-logs localstack-up localstack-down localstack-logs localstack-init-table clean-target compose-setup compose-build compose-up compose-down compose-restart compose-logs compose-health compose-clean

help:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║            FATEC AI Bot — Make Targets                        ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "🐳 Docker Compose (Recomendado - Integra tudo):"
	@echo "  make compose-setup        Preparar .env"
	@echo "  make compose-build        Build das imagens Docker"
	@echo "  make compose-up           Iniciar LocalStack + MCP + Backend"
	@echo "  make compose-down         Parar todos os serviços"
	@echo "  make compose-restart      Reiniciar todos os serviços"
	@echo "  make compose-logs         Ver logs de todos os serviços"
	@echo "  make compose-health       Checar saúde dos serviços"
	@echo "  make compose-clean        Remover containers e volumes"
	@echo ""
	@echo "🔨 Development (Local):"
	@echo "  make dev-image            Build development image"
	@echo "  make dev-up               Run Quarkus em dev mode (hot reload)"
	@echo "  make dev-shell            Abrir shell no dev container"
	@echo "  make dev-down             Stop dev container"
	@echo ""
	@echo "📦 Build & Production:"
	@echo "  make build-artifacts      Build artifacts Docker (target/quarkus-app)"
	@echo "  make prod-image           Build production image (native)"
	@echo "  make prod-up              Run production container"
	@echo "  make prod-down            Stop production container"
	@echo "  make prod-logs            Ver logs production"
	@echo ""
	@echo "🗄️  LocalStack (Legado - use compose-* agora):"
	@echo "  make localstack-up        Start LocalStack (DynamoDB only)"
	@echo "  make localstack-down      Stop LocalStack"
	@echo "  make localstack-logs      Tail LocalStack logs"
	@echo "  make localstack-tables    List DynamoDB tables"
	@echo ""
	@echo "🧹 Utilitários:"
	@echo "  make clean-target         Remove target/ directory"
	@echo ""

generate-keys:
	docker run --rm \
		-v "$(HOST_WORKSPACE):/workspace" \
		-w /workspace \
		alpine:latest \
		sh -c "apk add --no-cache openssl && \
		if [ ! -f src/main/resources/privateKey.pem ]; then \
			openssl genrsa -out src/main/resources/privateKey.pem 2048 && \
			openssl rsa -in src/main/resources/privateKey.pem -pubout -out src/main/resources/publicKey.pem && \
			echo 'RSA keys generated successfully'; \
		else \
			echo 'Keys already exist. Skipping generation.'; \
		fi"

dev-image:
	docker build -f Dockerfile.dev -t $(DEV_IMAGE) .

dev-up: dev-image
	-docker network create fatec-network >$(NULL_DEV) 2>&1
	docker run --rm -it \
		--name $(DEV_CONTAINER) \
		--network fatec-network \
		-p 8082:8080 \
		-p 5005:5005 \
		-e QUARKUS_DEVSERVICES_ENABLED=false \
		-e QUARKUS_DYNAMODB_ENDPOINT_OVERRIDE=http://localstack:4566 \
		-v "$(HOST_WORKSPACE):/workspace" \
		-v "$(M2_DIR):/root/.m2" \
		-w /workspace \
		$(DEV_IMAGE)

dev-shell: dev-image
	docker run --rm -it \
		--name $(DEV_CONTAINER)-shell \
		-v "$(HOST_WORKSPACE):/workspace" \
		-v "$(M2_DIR):/root/.m2" \
		-w /workspace \
		$(DEV_IMAGE) \
		bash

dev-down:
	@echo "Dev mode is running locally - use Ctrl+C to stop"

build-artifacts:
	docker run --rm \
		-v "$(HOST_WORKSPACE):/workspace" \
		-v "$(M2_DIR):/root/.m2" \
		-w /workspace \
		$(MAVEN_IMAGE) \
		mvn -DskipTests package

prod-image:
	docker build -f $(PROD_DOCKERFILE) -t $(PROD_IMAGE) .

prod-up: prod-image
	-@docker rm -f $(PROD_CONTAINER) >$(NULL_DEV) 2>&1
	docker run -d \
		--name $(PROD_CONTAINER) \
		-p 8081:8080 \
		$(PROD_IMAGE)

prod-down:
	-@docker rm -f $(PROD_CONTAINER) >$(NULL_DEV) 2>&1

prod-logs:
	docker logs -f $(PROD_CONTAINER)

localstack-up:
	docker compose up -d --build

localstack-down:
	docker compose down

localstack-logs:
	docker compose logs -f $(LOCALSTACK_SERVICE)

localstack-init-table:
	docker exec localstack awslocal dynamodb create-table \
		--table-name fatec-ai-bot-core \
		--attribute-definitions AttributeName=pk,AttributeType=S AttributeName=sk,AttributeType=S \
		--key-schema AttributeName=pk,KeyType=HASH AttributeName=sk,KeyType=RANGE \
		--billing-mode PAY_PER_REQUEST \
		--region us-east-1 || echo "Table already exists"

clean-target:
	rm -rf target

# ============================================================================
# Docker Compose Targets (Novos - Integra LocalStack + MCP + Backend)
# ============================================================================

compose-setup:
	@echo "📋 Preparando .env..."
	@if [ ! -f .env ]; then \
		cp .env.example .env; \
		echo "✅ .env criado"; \
		echo "⚠️  EDITE .env e adicione sua GROQ_API_KEY!"; \
	else \
		echo "✅ .env já existe"; \
	fi

compose-build: compose-setup
	@echo "🔨 Building imagens Docker..."
	docker-compose build
	@echo "✅ Build concluído!"

compose-up: compose-build
	@echo "🚀 Iniciando serviços (LocalStack + MCP + Backend)..."
	docker-compose up -d
	@echo "✅ Serviços iniciados!"
	@echo ""
	@sleep 3
	@make compose-health

compose-down:
	@echo "⛔ Parando serviços..."
	docker-compose down
	@echo "✅ Serviços parados!"

compose-restart:
	@echo "🔄 Reiniciando serviços..."
	docker-compose restart
	@echo "✅ Serviços reiniciados!"

compose-logs:
	docker-compose logs -f

compose-health:
	@echo "🏥 Checando saúde dos serviços..."
	@echo ""
	@echo -n "LocalStack: "
	@if curl -s http://localhost:4566 > /dev/null 2>&1; then \
		echo "✅ OK"; \
	else \
		echo "❌ DOWN"; \
	fi
	@echo -n "MCP Server: "
	@if curl -s http://localhost:8001/health > /dev/null 2>&1; then \
		echo "✅ OK"; \
	else \
		echo "❌ DOWN"; \
	fi
	@echo -n "Quarkus Backend: "
	@if curl -s http://localhost:8082/health > /dev/null 2>&1; then \
		echo "✅ OK"; \
	else \
		echo "❌ DOWN"; \
	fi
	@echo ""
	@echo "📡 Endereços:"
	@echo "   http://localhost:8082       (Quarkus)"
	@echo "   http://localhost:8082/swagger (Swagger UI)"
	@echo "   http://localhost:8001        (MCP Server)"
	@echo "   http://localhost:4566        (LocalStack)"

compose-clean:
	@echo "🗑️  Removendo containers e volumes..."
	docker-compose down -v
	@echo "✅ Limpeza concluída!"

compose-logs-backend:
	docker-compose logs -f quarkus-backend

compose-logs-mcp:
	docker-compose logs -f mcp-server

compose-logs-localstack:
	docker-compose logs -f localstack

compose-shell-backend:
	docker-compose exec quarkus-backend bash

compose-shell-mcp:
	docker-compose exec mcp-server bash

compose-ps:
	@echo "📋 Containers:"
	docker-compose ps
