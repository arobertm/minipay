.PHONY: infra infra-down dev-auth dev-payments dev-fraud dev-all \
        build-all test-unit logs clean ps help

# ─── Infrastructura ───────────────────────────────────────────
infra:
	docker compose -f docker-compose.infra.yml up -d
	@echo "Infra pornita. Kafka UI: http://localhost:8080 | Grafana: http://localhost:3001"

infra-down:
	docker compose -f docker-compose.infra.yml down

# ─── Servicii per domeniu ─────────────────────────────────────
dev-auth:
	docker compose --profile auth up --build

dev-payments:
	docker compose --profile payments up --build

dev-fraud:
	docker compose --profile fraud up --build

dev-all:
	docker compose --profile all up --build

# ─── Build ────────────────────────────────────────────────────
build-all:
	mvn clean package -DskipTests
	docker compose build

build-svc:
	mvn clean package -DskipTests -pl services/$(SVC)
	docker compose build $(SVC)

# ─── Teste ────────────────────────────────────────────────────
test-unit:
	mvn test

test-svc:
	mvn test -pl services/$(SVC)

# ─── Utilitare ────────────────────────────────────────────────
logs:
	docker compose logs -f

ps:
	docker compose ps

clean:
	docker compose -f docker-compose.infra.yml down -v
	docker compose down -v
	mvn clean

# ─── Help ─────────────────────────────────────────────────────
help:
	@echo ""
	@echo "  make infra          Porneste infrastructura (postgres, kafka, vault, grafana)"
	@echo "  make infra-down     Opreste infrastructura"
	@echo "  make dev-auth       Porneste serviciile Identity (auth, user, session)"
	@echo "  make dev-payments   Porneste serviciile Payments (gateway, vault, network, issuer, tds, minids)"
	@echo "  make dev-fraud      Porneste serviciile Security (fraud, audit, psd2, notif)"
	@echo "  make dev-all        Porneste TOATE serviciile"
	@echo "  make build-all      Build Maven + Docker toate serviciile"
	@echo "  make test-unit      Ruleaza toate unit testele"
	@echo "  make logs           Urmareste logurile"
	@echo "  make clean          Sterge tot (containere + volume + build)"
	@echo ""
	@echo "  Exemple:"
	@echo "  make build-svc SVC=auth-svc"
	@echo "  make test-svc SVC=gateway-svc"
	@echo ""
