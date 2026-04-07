# MiniPay — Payment Gateway

> Dissertation Project: *Analiza Comparativa a Sistemelor de Plata Digitale si Securitatea Informatiilor*

MiniPay este un Payment Gateway complet (similar Stripe), construit cu arhitectura microservicii pe Kubernetes, cu contributii originale in securitate post-cuantica, fraud detection explicabil si audit log imutabil.

---

## Arhitectura

```
12 microservicii Java 21 + Spring Boot 3
 1 serviciu Python 3.12 + FastAPI (fraud detection)
 1 frontend React + TypeScript
 MiniDS — storage distribuit MicroRaft + RocksDB
 Apache Kafka — messaging asincron
 Istio — service mesh mTLS
```

## Servicii

| Serviciu | Port | Rol |
|---|---|---|
| auth-svc | 8081 | OAuth2/OIDC + JWT Dilithium (PQC) |
| user-svc | 8082 | Utilizatori + RBAC |
| session-svc | 8083 | Sesiuni + revocare |
| gateway-svc | 8084 | API entry point merchant |
| vault-svc | 8085 | Tokenizare PAN → DPAN |
| network-svc | 8086 | Routing BIN + ISO 8583 |
| issuer-svc | 8087 | Simulator banca emitenta |
| tds-svc | 8088 | 3DS2 autentificare |
| settlement-svc | 8089 | Clearing zilnic |
| fraud-svc | 8090 | ML scoring + SHAP XAI |
| audit-svc | 8091 | Merkle Tree audit log |
| psd2-svc | 8092 | Open Banking PSD2 |
| notif-svc | 8093 | Notificari email/push |

## Pornire Rapida

```bash
# 1. Porneste infrastructura
make infra

# 2. Lucreaza la un domeniu specific
make dev-auth       # Identity services
make dev-payments   # Payment services
make dev-fraud      # Security services

# 3. Porneste tot (necesita 16GB+ RAM)
make dev-all

# 4. Ajutor
make help
```

## UI-uri Disponibile Local

| Serviciu | URL |
|---|---|
| Kafka UI | http://localhost:8080 |
| Grafana | http://localhost:3001 (admin/admin) |
| Prometheus | http://localhost:9090 |
| Vault UI | http://localhost:8200 (token: minipay-vault-token) |

## Contributii Originale

- **Post-Quantum Cryptography** — JWT semnat cu CRYSTALS-Dilithium (NIST FIPS 204)
- **Fraud XAI** — ML scoring cu SHAP explainability (GDPR Art. 22)
- **Merkle Tree Audit Log** — audit imutabil criptografic
- **MicroRaft + RocksDB** — storage distribuit cu consensus Raft
- **PSD2 Open Banking** — AIS/PIS complet implementat

## Tech Stack

```
Java 21 + Spring Boot 3.3
Spring Authorization Server 1.3
Bouncy Castle 1.78 (PQC)
MicroRaft 0.7 + RocksDB 9.0
Apache Kafka 3.7
PostgreSQL 16
HashiCorp Vault 1.15
Docker + K3s + Istio
```
