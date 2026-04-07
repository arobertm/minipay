# Disertatie — Analiza Comparativa a Sistemelor de Plata Digitale si Securitatea Informatiilor

> **Status:** In dezvoltare
> **Data start:** Aprilie 2026
> **Proiect practic:** MiniPay — Payment Gateway cu arhitectura microservicii pe Kubernetes

---

## 1. Structura Lucrarii

```
Cap. 1 — Introducere si Context
  - Evolutia platilor digitale (1970 → prezent)
  - Motivatia alegerii temei
  - Obiectivele lucrarii

Cap. 2 — Sisteme de Plata Digitala: Prezentare si Clasificare
  - Ce este Google Pay, Apple Pay (Digital Wallet)
  - Ce este Stripe, Netopia (Payment Gateway / Processor)
  - Lantul complet al unei plati
  - Arhitecturi tehnice comparate

Cap. 3 — Securitatea Informatiilor in Platile Digitale
  - Standarde: PCI DSS v4.0, ISO/IEC 27001, EMV
  - Tokenizare EMV, criptare E2E, 3DS2
  - Tipuri de atacuri: phishing, MITM, SIM swapping
  - GDPR si conformitate europeana
  - Post-Quantum Cryptography (NIST FIPS 203/204)

Cap. 4 — Analiza Comparativa
  - Google Pay vs Apple Pay vs Stripe vs Netopia
  - Tabel comparativ (tokenizare, biometrie, PCI DSS, PSD2)
  - Limitarile sistemelor existente
  - Justificarea contributiilor originale

Cap. 5 — MiniPay: Implementarea unui Payment Gateway
  - 5.1 Arhitectura sistemului
  - 5.2 MiniDS — storage distribuit cu MicroRaft + RocksDB
  - 5.3 MiniAM — OAuth2/OIDC custom (inspirat PingAM/PingDS)
  - 5.4 Token Vault — tokenizare EMV (PAN → DPAN)
  - 5.5 Fluxul de autorizare ISO 8583 simplificat
  - 5.6 3DS2 — autentificare suplimentara
  - 5.7 Fraud Detection cu XAI (SHAP explainability)
  - 5.8 Merkle Tree — audit log imutabil
  - 5.9 Post-Quantum Cryptography — JWT cu Dilithium
  - 5.10 PSD2 Open Banking API
  - 5.11 Testare si rezultate

Cap. 6 — Concluzii si Directii Viitoare
  - Rezultate obtinute
  - Comparatie cu sisteme reale
  - Directii: CBDC (Digital Euro), ZKP, Decentralized Identity
```

---

## 2. Surse si Lucrari de Citit

### Standarde Oficiale (OBLIGATORIU)
| # | Sursa | Unde gasesti |
|---|---|---|
| 1 | PCI DSS v4.0 (2022) | pcisecuritystandards.org |
| 2 | EMV Contactless Specifications | emvco.com/specifications |
| 3 | ISO/IEC 27001:2022 | iso.org |
| 4 | NIST SP 800-63B — Digital Identity | pages.nist.gov/800-63-3 |
| 5 | NIST FIPS 203 — Kyber (PQC) | csrc.nist.gov |
| 6 | NIST FIPS 204 — Dilithium (PQC) | csrc.nist.gov |
| 7 | PSD2 Directive EU 2015/2366 | eur-lex.europa.eu |
| 8 | GDPR EU 2016/679 | gdpr-info.eu |
| 9 | RFC 6749 — OAuth2 | rfc-editor.org |
| 10 | RFC 7636 — PKCE | rfc-editor.org |

### Carti
| # | Carte | Autor | An |
|---|---|---|---|
| 1 | Security Engineering (ed. 3) | Ross Anderson | 2020 |
| 2 | Applied Cryptography | Bruce Schneier | clasic |
| 3 | Designing Distributed Systems | Brendan Burns | 2018 |
| 4 | Microservices Patterns | Chris Richardson | 2018 |

### Articole IEEE / Scholar (cauta pe Google Scholar)
1. "A Comparative Study of Mobile Payment Systems Security" (2021-2024)
2. "Tokenization in Mobile Payments: EMV vs Host Card Emulation"
3. "3D Secure 2.0 Protocol Analysis and Security Evaluation"
4. "Post-Quantum Cryptography in Financial Systems"
5. "Explainable AI for Fraud Detection in Payment Systems"
6. "Raft: In Search of an Understandable Consensus Algorithm" — Ongaro & Ousterhout (2014)

### Rapoarte Industrie (gratuite)
- McKinsey Global Payments Report 2023/2024
- ECB — rapoarte plati digitale zona euro (ecb.europa.eu)
- BNR — rapoarte sisteme de plata Romania (bnr.ro)

---

## 3. Ce Este Fiecare Sistem (Clarificare)

```
Google Pay / Apple Pay = DIGITAL WALLET (aplicatia din telefon)
  - Stocheaza cardurile tale
  - Tokenizeaza cardul si il trimite mai departe
  - NU proceseaza ei plata
  - Sunt interfata CONSUMER

Stripe / Netopia = PAYMENT GATEWAY + PROCESSOR (infrastructura)
  - Primeste cererea de plata de la merchant
  - O trimite la bancile din spate
  - Returneaza rezultatul
  - Sunt interfata MERCHANT

TU construiesti un STRIPE — nu un Google Pay.
MiniPay = infrastructura din spate, nu aplicatia de telefon.

Lantul complet:
  Cumparator → [Google Pay] → Merchant → [MiniPay/Stripe] → Banca
                 wallet                    gateway/processor
```

---

## 4. Arhitectura MiniPay

### Diagrama Arhitecturala

```
                         INTERNET
                             |
                    [Ingress NGINX + TLS]
                             |
                    [Istio Service Mesh - mTLS]
                             |
        ┌────────────────────┼────────────────────┐
        |                    |                    |
   IDENTITY             PAYMENTS             SECURITY
   DOMAIN               DOMAIN               DOMAIN
        |                    |                    |
  auth-svc            gateway-svc           fraud-svc
  user-svc            vault-svc             audit-svc
  session-svc         network-svc           psd2-svc
                      issuer-svc            notif-svc
                      tds-svc
                      settlement-svc
                             |
              ┌──────────────┴──────────────┐
              |                             |
      [MiniDS StatefulSet]          [PostgreSQL per svc]
      MicroRaft + RocksDB           (date servicii individuale)
      Node-0 Leader
      Node-1 Replica
      Node-2 Replica
      (tokens, sessions, vault)
```

### Cele 12 Microservicii

| # | Serviciu | Domeniu | Rol | Tech |
|---|---|---|---|---|
| 1 | auth-svc | Identity | OAuth2/OIDC + JWT Dilithium | Spring Boot 3 |
| 2 | user-svc | Identity | Utilizatori + RBAC + Argon2id | Spring Boot 3 |
| 3 | session-svc | Identity | Sesiuni active + revocare | Spring Boot 3 |
| 4 | gateway-svc | Payments | API entry point merchant | Spring Boot 3 |
| 5 | vault-svc | Payments | Tokenizare PAN → DPAN (AES-256) | Spring Boot 3 |
| 6 | network-svc | Payments | Routing BIN + ISO 8583 | Spring Boot 3 |
| 7 | issuer-svc | Payments | Simulator banca emitenta | Spring Boot 3 |
| 8 | tds-svc | Payments | 3DS2 autentificare + OTP | Spring Boot 3 |
| 9 | settlement-svc | Payments | Clearing zilnic (CronJob K8s) | Spring Boot 3 |
| 10 | fraud-svc | Security | ML scoring + SHAP XAI | Python 3.12 + FastAPI |
| 11 | audit-svc | Security | Merkle Tree audit log imutabil | Spring Boot 3 |
| 12 | psd2-svc | Security | Open Banking PSD2 AIS/PIS | Spring Boot 3 |

### Comunicare Inter-Servicii

```
SINCRON (gRPC) — operatii critice timp real:
  gateway-svc  →  vault-svc      tokenizare card
  gateway-svc  →  fraud-svc      scor frauda
  gateway-svc  →  tds-svc        initiere 3DS2
  gateway-svc  →  network-svc    autorizare ISO 8583
  network-svc  →  issuer-svc     aprobare banca

ASINCRON (Kafka) — operatii non-critice:
  gateway-svc  →  audit-svc      log in Merkle Tree
  gateway-svc  →  notif-svc      email confirmare
  issuer-svc   →  settlement-svc clearing end-of-day
  fraud-svc    →  notif-svc      alerta frauda
```

---

## 5. Contributii Originale

Ce aduci in plus fata de Stripe, Google Pay, Apple Pay, Netopia:

| Contributie | Ce face | De ce conteaza |
|---|---|---|
| **Post-Quantum Crypto** | JWT semnat cu CRYSTALS-Dilithium (NIST FIPS 204) | Stripe/Google Pay inca folosesc RSA/ECDSA, vulnerabile la quantum computing |
| **Fraud XAI (SHAP)** | ML scoring cu explicatie pentru fiecare decizie | GDPR Art.22 — dreptul la explicatie; Stripe Radar e black-box |
| **Merkle Tree Audit** | Log imutabil criptografic (hash chain) | Orice alterare a istoricului e detectabila instant |
| **MicroRaft + RocksDB** | Storage distribuit cu consensus Raft | Arhitectura transparenta si documentata vs black-box |
| **PSD2 Open Banking** | AIS/PIS complet implementat | Netopia nu are; Stripe partial |

---

## 6. Stack Tehnologic Complet

```
Backend (11 servicii Java):
  Java 21 (Virtual Threads — Project Loom)
  Spring Boot 3.3
  Spring Authorization Server 1.3 (OAuth2/OIDC)
  Spring Security 6
  Spring Data JPA
  Spring Kafka 3.3
  grpc-spring-boot-starter 3.1 (comunicare inter-svc)
  Bouncy Castle 1.78 (PQC — Dilithium/Kyber)
  MicroRaft 0.7 (Raft consensus)
  RocksDB 9.0 (embedded KV store)

Fraud Service (Python):
  Python 3.12
  FastAPI + uvicorn
  scikit-learn + XGBoost
  SHAP (explainability)
  grpcio

Storage:
  PostgreSQL 16 (date per serviciu)
  MicroRaft + RocksDB (MiniDS — tokens, sessions, vault)

Messaging:
  Apache Kafka 3.7 (3 brokeri, replication factor 3)

Secrets:
  HashiCorp Vault 1.15

Frontend:
  React 18 + TypeScript
  TailwindCSS

Containerizare (local):
  Docker Desktop
  Docker Compose cu profiles

Orchestrare (VPS/Cloud):
  K3s (Kubernetes lightweight)
  Istio (service mesh, mTLS)
  Helm (charts per domeniu)

CI/CD:
  GitHub Actions
  GitHub Container Registry (imagini Docker, gratuit)

Observability:
  Prometheus + Grafana
  Jaeger (distributed tracing)
```

---

## 7. MiniDS — De Ce MicroRaft + RocksDB

```
Optiune              | Verdict
---------------------|--------------------------------------------------
H2 + Custom Raft     | EVITA — implementare Raft de la zero = luni
                     | de debugging, bugs garantate (split brain etc.)
Kafka replication    | EVITA ca DB — eventual consistency, nu ACID,
                     | anti-pattern pentru storage tranzactional
PostgreSQL + Patroni | Corect pentru productie dar nu demonstrezi
                     | nimic original in lucrare
MicroRaft + RocksDB  | ALES — Raft real (algoritm Ongaro 2014),
                     | Apache 2.0, similar CockroachDB/etcd,
                     | cod clar explicabil in Cap. 5
```

### Cum Functioneaza Raft (pentru lucrare)

```
3 noduri: Node-0 (Leader), Node-1 (Replica), Node-2 (Replica)

LEADER ELECTION:
  Toti pornesc ca Followers
  Primul care nu primeste heartbeat → Candidate → cere voturi
  Majority (2/3) → devine Leader

LOG REPLICATION (o scriere):
  Client: "salveaza token T123"
  Leader: adauga in WAL log → trimite AppendEntries la replici
  2/3 ACK → COMMIT → aplica in RocksDB → raspunde client

FAILOVER:
  Leader cade → noua electie in < 300ms → alt nod devine Leader
  Datele sunt safe: orice entry COMMITTED era deja pe majority
```

---

## 8. Fluxul Complet al unei Plati

```
1.  User        →  POST /v1/payments/authorize
2.  API Gateway →  auth-svc: valideaza JWT (Dilithium)
3.  gateway-svc    primeste requestul
4.  gateway-svc →  vault-svc:    PAN → DPAN (tokenizare)
5.  gateway-svc →  fraud-svc:    calculeaza scor frauda
                   score > 0.8   → BLOCKED + SHAP explanation
                   score > 0.5   → trigger 3DS2 challenge
6.  gateway-svc →  tds-svc:      3DS2 (daca necesar)
                   Frictionless  → scor mic, aproba automat
                   Challenge     → OTP catre user
7.  gateway-svc →  network-svc:  AuthorizationRequest ISO 8583
8.  network-svc →  issuer-svc:   verifica card + limita + fraud
                   response: 00=APPROVED / 51=INSUF / 05=DECLINED
9.  network-svc →  gateway-svc:  raspuns
10. gateway-svc →  Kafka:        event → audit-svc + notif-svc
11. gateway-svc →  User:         { status: APPROVED, txn_id }

Timp total tinta: < 300ms
```

---

## 9. Testare — Fara Banca Reala

### De ce nu ai nevoie de banca reala
```
Conectarea la retele reale Visa/MC necesita autorizatie BNR
ca Institutie de Plata (Legea 209/2019) + audit PCI DSS L1.
Imposibil pentru o disertatie.
```

### Carduri de Test Standard (public)
```
VISA approved:         4111 1111 1111 1111
VISA always declined:  4000 0000 0000 0002
VISA insuf. funds:     4000 0000 0000 9995
MC approved:           5500 0000 0000 0004
CVV: orice 3 cifre  |  Expiry: orice data viitoare
```

### Response Codes ISO 8583
```
00 → Approved
05 → Do Not Honor (fraud / card blocat)
14 → Invalid Card Number
51 → Insufficient Funds
54 → Expired Card
65 → Activity Limit Exceeded
```

### Sandbox-uri Externe (optional pentru psd2-svc)
```
ING Developer Portal  → developer.ing.com     (cel mai bun)
BCR Developer Portal  → developer.bcr.ro
Visa Developer Center → developer.visa.com
Toate gratuite, inregistrare cu email.
```

### Scenarii Demo Prezentare
```
1. Plata normala        → APPROVED < 300ms
2. Fraud detectat       → score 0.87, BLOCKED + SHAP reasons afisat
3. 3DS Challenge        → OTP generat → introdus → APPROVED
4. MiniDS failover      → opresti Leader live → replica preia
5. Merkle Tree demo     → incerci sa modifici un log → detectat instant
6. PQC demo             → JWT Dilithium vs RSA comparison
```

---

## 10. Mediul Local de Dezvoltare

### Principiu de Baza
```
NU pornesti toate serviciile simultan pe laptop.
Lucrezi la UN serviciu odata + infra minima.
Deploy complet → pe VPS.
```

### Structura Repository

```
minipay/
├── services/
│   ├── auth-svc/
│   │   ├── src/main/java/ro/minipay/auth/
│   │   ├── src/test/
│   │   ├── Dockerfile
│   │   └── pom.xml
│   ├── gateway-svc/
│   ├── vault-svc/
│   ├── network-svc/
│   ├── issuer-svc/
│   ├── tds-svc/
│   ├── user-svc/
│   ├── session-svc/
│   ├── audit-svc/
│   ├── settlement-svc/
│   ├── psd2-svc/
│   ├── notif-svc/
│   └── fraud-svc/              ← Python FastAPI
│       ├── app/
│       ├── requirements.txt
│       └── Dockerfile
├── minids/                     ← MicroRaft + RocksDB
│   ├── src/
│   └── pom.xml
├── frontend/                   ← React + TypeScript
├── proto/                      ← .proto gRPC partajate
│   ├── auth.proto
│   ├── payment.proto
│   └── fraud.proto
├── k8s/                        ← Helm charts
│   ├── identity/
│   ├── payments/
│   ├── security/
│   └── data/
├── docker-compose.infra.yml    ← postgres + kafka + vault
├── docker-compose.yml          ← servicii cu profiles
└── Makefile                    ← comenzi rapide
```

### Profiles Docker Compose

```
make infra           → porneste: postgres, kafka, vault, zookeeper
make dev-auth        → infra + auth-svc + user-svc + session-svc
make dev-payments    → infra + gateway + vault-svc + network + issuer + tds
make dev-fraud       → infra + fraud-svc + audit-svc
make dev-all         → tot (necesita 16GB+ RAM)
make test-unit       → ruleaza unit tests pentru un serviciu
make test-e2e        → porneste tot + ruleaza scenariile de test
make logs            → docker compose logs -f
make clean           → docker compose down -v
```

### Porturi Locale

```
auth-svc:        8081
user-svc:        8082
session-svc:     8083
gateway-svc:     8084
vault-svc:       8085
network-svc:     8086
issuer-svc:      8087
tds-svc:         8088
settlement-svc:  8089
fraud-svc:       8090  (Python)
audit-svc:       8091
psd2-svc:        8092
notif-svc:       8093
minids-0:        8300 (Raft) / 8301 (API)
minids-1:        8310 (Raft) / 8311 (API)
minids-2:        8320 (Raft) / 8321 (API)
postgres:        5432
kafka:           9092
vault:           8200
frontend:        3000
grafana:         3001
```

---

## 11. Deploy pe VPS

### Optiune 1 — Oracle Cloud Free Tier (recomandat)
```
Resurse GRATUITE PERMANENT:
  VM-1: 2 OCPU ARM + 12GB RAM → K3s master
  VM-2: 2 OCPU ARM + 12GB RAM → K3s worker

Setup:
  1. cloud.oracle.com → Create Account (card credit, nu taxeaza)
  2. Always Free → VM.Standard.A1.Flex
  3. curl -sfL https://get.k3s.io | sh -   (pe ambele VM-uri)
  4. GitHub Actions → build + push → kubectl apply
```

### Optiune 2 — VPS Platit (daca Oracle nu merge)
```
Contabo VPS M: 6 vCPU + 16GB RAM = 8.99€/luna
  → x86 (fara probleme ARM cu imagini Docker)
  → suficient pentru tot cluster-ul
```

### GitHub Actions Workflow
```
git push origin main
       ↓
GitHub Actions:
  mvn test (unit tests toate serviciile)
  docker build + push → ghcr.io/[username]/minipay
  kubectl apply → K3s (Oracle / VPS)
       ↓
Rolling update fara downtime
```

---

## 12. Plan de Lucru

### Luna 1 — Cercetare + Setup Proiect
```
[ ] Citit: PCI DSS v4.0 (rezumat executive)
[ ] Citit: RFC 6749 + RFC 7636 (OAuth2 + PKCE)
[ ] Citit: Raft paper — Ongaro 2014 (18 pagini, esential)
[ ] Citit: NIST FIPS 204 (Dilithium — sectiunile intro)
[ ] Scris: Cap. 1 — Introducere
[ ] Scris: Cap. 2 — Google Pay / Apple Pay / Stripe / Netopia
[ ] Setup: GitHub repo + structura mono-repo
[ ] Setup: docker-compose.infra.yml functional
[ ] Setup: Maven parent POM + module structure
```

### Luna 2 — MiniDS + MiniAM (Identity)
```
[ ] MiniDS: MicroRaft + RocksDB implementare (StateMachine)
[ ] MiniDS: test failover live (demo ca merge)
[ ] auth-svc: Spring Authorization Server setup
[ ] auth-svc: JWT cu Dilithium (Bouncy Castle PQC)
[ ] user-svc: CRUD utilizatori + Argon2id password hashing
[ ] session-svc: sesiuni + revocare
[ ] Scris: Cap. 3 — Securitate
[ ] Scris: Cap. 4 — Analiza Comparativa
```

### Luna 3 — Payment Core
```
[ ] vault-svc: tokenizare PAN → DPAN (AES-256-GCM)
[ ] gateway-svc: API complet (/authorize, /capture, /refund)
[ ] network-svc: routing BIN + mesaje ISO 8583
[ ] issuer-svc: simulator banca (toate response codes)
[ ] tds-svc: flux 3DS2 (frictionless + challenge OTP)
[ ] Test: flux complet de plata end-to-end
```

### Luna 4 — Security Features + Frontend
```
[ ] fraud-svc: ML model antrenat (Python + XGBoost)
[ ] fraud-svc: SHAP explainability integrat
[ ] audit-svc: Merkle Tree implementare
[ ] psd2-svc: Open Banking AIS/PIS
[ ] notif-svc: email + push notifications
[ ] settlement-svc: CronJob clearing zilnic
[ ] Frontend: React UI pentru demo
```

### Luna 5 — Deploy + Finalizare Lucrare
```
[ ] Setup VPS (Oracle Cloud sau Contabo)
[ ] K3s cluster + Helm charts deploy
[ ] GitHub Actions CI/CD pipeline
[ ] Prometheus + Grafana dashboards
[ ] Scris: Cap. 5 — documentatie proiect (cu diagrame)
[ ] Scris: Cap. 6 — concluzii
[ ] Revizuire integrala lucrare
[ ] Pregatire prezentare + demo live
```

---

## 13. Pasi Imediat Urmatori

```
[ ] 1. Creeaza repo GitHub: github.com/[username]/minipay
[ ] 2. Initializeaza mono-repo cu Maven parent POM
[ ] 3. Scrie docker-compose.infra.yml
       (postgres:16, kafka:3.7, vault:1.15)
[ ] 4. Verifica: docker compose -f docker-compose.infra.yml up
[ ] 5. Creeaza scheletul auth-svc (Spring Boot 3 + Spring Auth Server)
[ ] 6. Primul endpoint functional: POST /oauth2/token
[ ] 7. Adauga Bouncy Castle + primul JWT semnat cu Dilithium
```

---

## 14. Dependinte Maven Principale

```xml
<!-- Parent POM -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.4</version>
</parent>

<!-- OAuth2 Authorization Server -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-authorization-server</artifactId>
    <version>1.3.2</version>
</dependency>

<!-- gRPC -->
<dependency>
    <groupId>net.devh</groupId>
    <artifactId>grpc-spring-boot-starter</artifactId>
    <version>3.1.0.RELEASE</version>
</dependency>

<!-- Post-Quantum Cryptography (Bouncy Castle) -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
    <version>1.78.1</version>
</dependency>
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcpkix-jdk18on</artifactId>
    <version>1.78.1</version>
</dependency>

<!-- MicroRaft -->
<dependency>
    <groupId>io.microraft</groupId>
    <artifactId>microraft</artifactId>
    <version>0.7</version>
</dependency>

<!-- RocksDB -->
<dependency>
    <groupId>org.rocksdb</groupId>
    <artifactId>rocksdbjni</artifactId>
    <version>9.0.0</version>
</dependency>

<!-- Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```
