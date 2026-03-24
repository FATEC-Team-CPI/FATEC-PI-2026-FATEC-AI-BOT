APP_NAME := fatec-ai-bot-backend

DEV_IMAGE := $(APP_NAME):dev
PROD_IMAGE := $(APP_NAME):prod

DEV_CONTAINER := $(APP_NAME)-dev
PROD_CONTAINER := $(APP_NAME)-prod

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

.PHONY: help dev-image dev-up dev-shell dev-down build-artifacts prod-image prod-up prod-down prod-logs clean-target

help:
	@echo "Targets available:"
	@echo "  make dev-image        Build development image"
	@echo "  make dev-up           Run Quarkus in dev mode (hot reload)"
	@echo "  make dev-shell        Open shell in dev image with project mounted"
	@echo "  make dev-down         Stop dev container"
	@echo "  make build-artifacts  Build Quarkus artifacts in Docker (target/quarkus-app)"
	@echo "  make prod-image       Build production image (native, lightweight)"
	@echo "  make prod-up          Run production container in background"
	@echo "  make prod-down        Stop and remove production container"
	@echo "  make prod-logs        Tail production container logs"
	@echo "  make clean-target     Remove local target directory"

dev-image:
	docker build -f Dockerfile.dev -t $(DEV_IMAGE) .

dev-up: dev-image
	docker run --rm -it \
		--name $(DEV_CONTAINER) \
		-p 8080:8080 \
		-p 5005:5005 \
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
	-docker stop $(DEV_CONTAINER)

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
		-p 8080:8080 \
		$(PROD_IMAGE)

prod-down:
	-@docker rm -f $(PROD_CONTAINER) >$(NULL_DEV) 2>&1

prod-logs:
	docker logs -f $(PROD_CONTAINER)

clean-target:
	rm -rf target
