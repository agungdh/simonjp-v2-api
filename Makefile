# Helper dev SIMONJP (lihat compose.yml).
COMPOSE ?= docker compose

.DEFAULT_GOAL := help

.PHONY: help up down logs reset run test build db-shell

help: ## daftar target
	@grep -hE '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS=":.*?## "}; {printf "  \033[36m%-9s\033[0m %s\n", $$1, $$2}'

up: ## nyalain container (postgres, valkey, minio, adminer, redis-commander) + tunggu sampai healthy
	$(COMPOSE) up -d --wait

down: ## matiin container (volume tetap ada)
	$(COMPOSE) down

logs: ## ikutin log container
	$(COMPOSE) logs -f

reset: ## reset total: down -v (semua volume kehapus, termasuk DB + object storage) lalu up -d
	$(COMPOSE) down -v
	$(COMPOSE) up -d --wait
	@echo "Container & volume sudah dibuang, DB dev mulai dari nol. Lanjut: make run"

run: ## jalanin app di http://localhost:8080
	./gradlew bootRun

test: ## test (butuh postgres + valkey hidup)
	./gradlew test

build: ## build jar tanpa test (sama seperti CI)
	./gradlew bootJar -x test

db-shell: ## buka psql interaktif ke DB dev
	$(COMPOSE) exec postgres psql -U admin -d simonjp
