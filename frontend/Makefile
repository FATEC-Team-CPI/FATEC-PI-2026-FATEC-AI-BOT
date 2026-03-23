DOCKER ?= docker

DEV_IMAGE ?= fatec-vue-dev
PROD_IMAGE ?= fatec-vue-prod

DEV_CONTAINER ?= fatec-vue-dev
PROD_CONTAINER ?= fatec-vue-prod

DEV_PORT ?= 5173
PROD_PORT ?= 8080
CHOKIDAR_USEPOLLING ?= true
CHOKIDAR_INTERVAL ?= 100

PROJECT_DIR := $(CURDIR)

.PHONY: help build-dev run-dev stop-dev restart-dev logs-dev shell-dev \
	build-prod run-prod stop-prod restart-prod logs-prod shell-prod \
	ps images clean prune prune-all

help:
	@echo "Available targets:"
	@echo "  make build-dev    Build dev image (Dockerfile.dev)"
	@echo "  make run-dev      Run dev container with volume mount and hot reload"
	@echo "                   Uses polling vars for Windows/WSL stability"
	@echo "  make stop-dev     Stop and remove dev container"
	@echo "  make restart-dev  Restart dev container"
	@echo "  make logs-dev     Show dev container logs"
	@echo "  make shell-dev    Open shell inside dev container"
	@echo "  make build-prod   Build production image (Dockerfile.prod)"
	@echo "  make run-prod     Run production container in background"
	@echo "  make stop-prod    Stop and remove production container"
	@echo "  make restart-prod Restart production container"
	@echo "  make logs-prod    Show production container logs"
	@echo "  make shell-prod   Open shell inside production container"
	@echo "  make ps           List running containers"
	@echo "  make images       List project images"
	@echo "  make clean        Remove project containers and images"
	@echo "  make prune        Remove project containers and images (safe)"
	@echo "  make prune-all    Remove all unused Docker resources (global)"

build-dev:
	$(DOCKER) build -f Dockerfile.dev -t $(DEV_IMAGE) .

run-dev: stop-dev build-dev
	$(DOCKER) run --rm -it \
		--name $(DEV_CONTAINER) \
		-e CHOKIDAR_USEPOLLING=$(CHOKIDAR_USEPOLLING) \
		-e CHOKIDAR_INTERVAL=$(CHOKIDAR_INTERVAL) \
		-p $(DEV_PORT):5173 \
		-v "$(PROJECT_DIR):/app" \
		-v /app/node_modules \
		$(DEV_IMAGE)

stop-dev:
	-$(DOCKER) rm -f $(DEV_CONTAINER)

restart-dev: stop-dev run-dev

logs-dev:
	$(DOCKER) logs -f $(DEV_CONTAINER)

shell-dev:
	$(DOCKER) exec -it $(DEV_CONTAINER) sh

build-prod:
	$(DOCKER) build -f Dockerfile.prod -t $(PROD_IMAGE) .

run-prod: stop-prod build-prod
	$(DOCKER) run -d \
		--name $(PROD_CONTAINER) \
		-p $(PROD_PORT):80 \
		$(PROD_IMAGE)

stop-prod:
	-$(DOCKER) rm -f $(PROD_CONTAINER)

restart-prod: stop-prod run-prod

logs-prod:
	$(DOCKER) logs -f $(PROD_CONTAINER)

shell-prod:
	$(DOCKER) exec -it $(PROD_CONTAINER) sh

ps:
	$(DOCKER) ps --filter "name=$(DEV_CONTAINER)" --filter "name=$(PROD_CONTAINER)"

images:
	$(DOCKER) images | grep -E "$(DEV_IMAGE)|$(PROD_IMAGE)" || true

clean: stop-dev stop-prod
	-$(DOCKER) rmi $(DEV_IMAGE) $(PROD_IMAGE)

prune:
	@echo "Prune seguro: removendo apenas recursos deste projeto..."
	$(MAKE) clean

prune-all:
	$(DOCKER) system prune -f
