# MiniPay — Documentatie Tehnica Completa
### Disertatie: Analiza Comparativa a Sistemelor de Plata Digitale si Securitatea Informatiilor

> **Status proiect:** FINALIZAT (Mai 2026)
> **Backend live:** `https://api-minipay.online` (DigitalOcean, k3s)
> **Frontend:** Vercel (deploy in curs)
> **Repository:** `https://github.com/arobertm/minipay`

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
  - 5.3 auth-svc — OAuth2/OIDC cu Post-Quantum Cryptography
  - 5.4 user-svc, session-svc — Identity Management
  - 5.5 vault-svc — Tokenizare EMV (PAN → DPAN)
  - 5.6 gateway-svc — Orchestrare plati
  - 5.7 network-svc + issuer-svc — Simulare retea bancara
  - 5.8 fraud-svc — ML Fraud Detection cu XAI (SHAP)
  - 5.9 audit-svc — Merkle Tree Audit Log imutabil
  - 5.10 tds-svc — 3DS2 Authentication
  - 5.11 settlement-svc + notif-svc + psd2-svc
  - 5.12 Frontend Next.js Dashboard
  - 5.13 Infrastructura: K8s, CI/CD, Monitoring
  - 5.14 Testare si rezultate

Cap. 6 — Concluzii si Directii Viitoare
  - Rezultate obtinute
  - Comparatie cu sisteme reale
  - Directii: CBDC (Digital Euro), ZKP, Decentralized Identity
```

---

## 2. Ce Este MiniPay

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

MiniPay = infrastructura din spate (Payment Gateway)
  - Similar Stripe, nu Google Pay
  - Lantul complet: Cumparator → [Google Pay] → Merchant → [MiniPay] → Banca
```

**MiniPay** implementeaza un Payment Gateway complet cu 14 microservicii, acoperind:
- Identity & Access Management (OAuth2/OIDC, JWT post-quantum)
- Tokenizare EMV (PAN → DPAN, AES-256-GCM)
- Fraud Detection ML (XGBoost + SHAP explainability)
- Audit imutabil (SHA-256 hash chain)
- 3DS2 Authentication (ACS simulator)
- Open Banking PSD2 (AIS + PIS)
- Clearing zilnic (settlement batches)
- Notificari (email + push)

---

## 3. Arhitectura Sistemului

### 3.1 Diagrama arhitecturala

```
                            INTERNET
                                |
                    ┌───────────┴───────────┐
                    │  Traefik Ingress       │
                    │  api-minipay.online    │
                    │  TLS termination       │
                    └───────────┬───────────┘
                                |
         ┌──────────────────────┼──────────────────────┐
         |                      |                      |
         ▼                      ▼                      ▼
  ┌─────────────┐       ┌──────────────┐       ┌──────────────┐
  │  IDENTITY   │       │   PAYMENTS   │       │  SECURITY    │
  ├─────────────┤       ├──────────────┤       ├──────────────┤
  │ auth-svc    │       │ gateway-svc  │       │ fraud-svc    │
  │ user-svc    │       │ vault-svc    │       │ audit-svc    │
  │ session-svc │       │ network-svc  │       │ notif-svc    │
  └──────┬──────┘       │ issuer-svc   │       │ tds-svc      │
         |              │ settlement   │       └──────────────┘
         |              │ psd2-svc     │
         |              └──────┬───────┘
         |                     |
         └─────────────────────┘
                      |
         ┌────────────┴────────────────────────┐
         |                                     |
┌────────┴──────────┐              ┌───────────┴──────────┐
│   MiniDS Cluster  │              │  PostgreSQL + Kafka   │
│   (MicroRaft 0.7) │              │                      │
├───────────────────┤              │  PostgreSQL 16:      │
│ minids-0 (Leader) │              │  - issuer-svc        │
│ minids-1 (Replica)│              │  - audit-svc         │
│ minids-2 (Replica)│              │  - tds-svc           │
├───────────────────┤              │  - settlement-svc    │
│ ou=users          │              │                      │
│ ou=sessions       │              │  Kafka 3.7 (KRaft):  │
│ ou=clients        │              │  - payment-events    │
│ ou=vault          │              │  - audit-events      │
└───────────────────┘              └──────────────────────┘
```

### 3.2 Porturi servicii

| Serviciu | Port | Domeniu |
|---|---|---|
| auth-svc | 8081 | Identity |
| user-svc | 8082 | Identity |
| session-svc | 8083 | Identity |
| gateway-svc | 8084 | Payments |
| vault-svc | 8085 | Payments |
| network-svc | 8086 | Payments |
| issuer-svc | 8087 | Payments |
| tds-svc | 8088 | Payments |
| settlement-svc | 8094 | Payments |
| psd2-svc | 8095 | Payments |
| fraud-svc | 8090 | Security (Python) |
| audit-svc | 8091 | Security |
| notif-svc | 8093 | Security |
| minids-0 | 8301 | Data |
| minids-1 | 8311 | Data |
| minids-2 | 8321 | Data |
| PostgreSQL | 5432 | Data |
| Kafka | 9092 | Data |
| Prometheus | 9090 | Monitoring |
| Grafana | 3000 | Monitoring |
| Kafka UI | 8080 | Monitoring |

### 3.3 Comunicare inter-servicii

```
SINCRON (REST HTTP) — operatii critice:
  gateway-svc  →  vault-svc      tokenizare PAN → DPAN
  gateway-svc  →  fraud-svc      scor frauda (XGBoost)
  gateway-svc  →  network-svc    autorizare ISO 8583
  network-svc  →  vault-svc      detokenizare DPAN → PAN
  network-svc  →  issuer-svc     aprobare banca emitenta

ASINCRON (Kafka) — operatii non-critice:
  gateway-svc  →  [payment-events]  →  audit-svc    (hash chain)
  gateway-svc  →  [payment-events]  →  notif-svc    (email/push)
  gateway-svc  →  [payment-events]  →  settlement   (clearing)
```

---

## 4. Servicii — Descriere Detaliata

---

### 4.1 MiniDS — Directory Server Distribuit

**Port:** 8301–8321 (3 noduri)
**Locatie:** `minids/`

#### Responsabilitate
MiniDS este un **directory server distribuit** inspirat din PingDS (Ping Identity). Este singurul storage pentru datele de identitate: utilizatori, sesiuni, clienti OAuth2, tokene vault. Implementeaza consensul **Raft** pentru consistenta distribuita si **RocksDB** embedded pentru persistenta.

#### Tehnologii
| Tehnologie | Versiune | Rol |
|---|---|---|
| Spring Boot | 3.4.4 | Framework |
| MicroRaft | 0.7 | Raft consensus algorithm |
| RocksDB | 9.0.0 | Key-Value storage embedded |
| Java | 21 (Virtual Threads) | Runtime |

#### Arhitectura Raft
```
3 noduri: minids-0 (Leader), minids-1 (Replica), minids-2 (Replica)

LEADER ELECTION:
  - Toti pornesc ca Followers
  - Primul fara heartbeat → Candidate → cere voturi
  - Majority (2/3) → devine Leader

LOG REPLICATION (scriere):
  Client: POST /minids/v1/entries
  Leader → adauga in WAL → trimite AppendEntries la replici
  2/3 ACK → COMMIT → aplica in RocksDB → raspunde client

FAILOVER:
  Leader cade → noua electie in ~300ms → alta replica preia
  Date safe: orice COMMITTED era deja pe majority
```

#### Endpoint-uri REST
```
GET    /minids/v1/entries/{dn}          citeste o inregistrare
POST   /minids/v1/entries               creeaza inregistrare (trece prin Raft)
PUT    /minids/v1/entries/{dn}          inlocuieste complet
PATCH  /minids/v1/entries/{dn}          actualizare partiala
DELETE /minids/v1/entries/{dn}          sterge inregistrare
POST   /minids/v1/search                cautare cu filtru
POST   /raft/message                    transport Raft intern (HTTP pe port API)
```

#### Model de date (LDAP-inspired)
```json
{
  "dn": "uid=ion.popescu,ou=users,dc=minipay,dc=ro",
  "objectClass": "minipayUser",
  "attributes": {
    "uid": "ion.popescu",
    "mail": "ion@example.com",
    "cn": "Ion Popescu",
    "userPassword": "$argon2id$v=19$...",
    "status": "ACTIVE"
  }
}
```

#### Organizatii (ou=)
```
ou=users     → utilizatori (user-svc)
ou=sessions  → sesiuni dispozitiv (session-svc)
ou=clients   → clienti OAuth2 (auth-svc)
ou=vault     → tokene DPAN criptate (vault-svc)
```

---

### 4.2 auth-svc — OAuth2 / OIDC cu Post-Quantum Cryptography

**Port:** 8081
**Locatie:** `services/auth-svc/`

#### Responsabilitate
Serviciu OAuth2/OIDC complet bazat pe **Spring Authorization Server 1.3.2**. Emite JWT-uri semnate cu RSA-2048 (standard) si JWT-uri semnate cu **CRYSTALS-Dilithium3** (post-quantum). Contributia originala principala a lucrarii.

#### Tehnologii
| Tehnologie | Versiune | Rol |
|---|---|---|
| Spring Authorization Server | 1.3.2 | OAuth2/OIDC framework |
| Bouncy Castle | 1.78.1 | PQC — CRYSTALS-Dilithium3 |
| Spring Security | 6.x | Resource server, JWT validation |
| Java | 21 | Runtime |

#### Endpoint-uri
```
POST   /oauth2/token                    emite JWT (RS256, standard OAuth2)
POST   /oauth2/introspect               valideaza token
POST   /oauth2/revoke                   revoaca token
GET    /oauth2/jwks                     JWKS cu RSA-2048 + Dilithium3
POST   /auth/token/pqc                  emite JWT semnat Dilithium3
POST   /auth/token/pqc/verify           verifica JWT Dilithium3
GET    /oauth2/server-metadata-pqc      OIDC discovery extins (include PQC)
```

#### Post-Quantum Cryptography — CRYSTALS-Dilithium3
```
Dilithium3 este algoritmul de semnatura digitala NIST FIPS 204 (2024).
Este rezistent la atacuri cu calculator cuantic (Shor's algorithm).

Comparatie RSA-2048 vs Dilithium3:
  RSA-2048:    cheie publica 256 bytes,  semnatura 256 bytes   (vulnerabil quantum)
  Dilithium3:  cheie publica 1952 bytes, semnatura 3293 bytes  (rezistent quantum)

Implementare: Bouncy Castle 1.78.1 cu BCDilithiumPublicKey / BCDilithiumPrivateKey

JWKS extins (raspuns /oauth2/jwks):
  {
    "keys": [
      { "kty": "RSA", "alg": "RS256", "kid": "rsa-2048-1", ... },
      {
        "kty": "OKP",
        "alg": "DILITHIUM3",
        "crv": "Dilithium3",
        "kid": "dil3-1",
        "x": "base64url(publicKeyBytes)",
        "key_size": 1952,
        "nist_level": 3,
        "signature_size_bytes": 3293
      }
    ]
  }

De ce conteaza: Stripe, Google Pay, Apple Pay inca folosesc RSA/ECDSA.
Un calculator cuantic suficient de mare ar sparge semnaturile RSA in ore.
MiniPay este pregatit pentru era post-quantum.
```

---

### 4.3 user-svc — Management Utilizatori

**Port:** 8082
**Locatie:** `services/user-svc/`

#### Responsabilitate
CRUD utilizatori cu hashing parola **Argon2id** (recomandat OWASP 2024) si RBAC (USER / ADMIN). Stocheaza utilizatorii in MiniDS.

#### Tehnologii
- Spring Boot 3.4.4 + Spring Security (Resource Server JWT)
- Argon2id (password hashing)
- MiniDS client (REST)

#### Endpoint-uri
```
POST   /users/users                     inregistrare utilizator nou
GET    /users/users/{userId}            citeste profil
GET    /users/users?email=...           cautare dupa email
PUT    /users/users/{userId}            actualizeaza profil
POST   /users/users/{userId}/change-password  schimba parola
DELETE /users/users/{userId}            stergere GDPR (soft-delete)
```

#### Model
```java
User {
  userId:       UUID
  email:        String (unic)
  passwordHash: String (Argon2id — NICIODATA stocat plaintext)
  firstName:    String
  lastName:     String
  role:         USER | ADMIN
  status:       ACTIVE | SUSPENDED | DELETED
  createdAt:    ISO-8601
}
```

#### Argon2id — De ce nu BCrypt
```
BCrypt (2014): memory 4KB — GPU crack < $1/milion parole
Argon2id (2015, castigator PHC): memory 64MB+ — GPU crack imposibil practic

Parametri MiniPay: memory=19MB, iterations=2, parallelism=1
Rezistent la: brute force, dictionary attacks, GPU/ASIC attacks
```

---

### 4.4 session-svc — Management Sesiuni Dispozitiv

**Port:** 8083
**Locatie:** `services/session-svc/`

#### Responsabilitate
Crearea, revocarea si expirarea sesiunilor de dispozitiv. Suporta multi-device login. Stocheaza sesiunile in MiniDS.

#### Endpoint-uri
```
POST   /sessions                        creeaza sesiune (dupa login)
GET    /sessions/{sessionId}            citeste sesiune
GET    /sessions?userId=...             listeaza sesiuni active user
POST   /sessions/{sessionId}/touch      actualizeaza lastSeenAt
DELETE /sessions/{sessionId}            revoaca sesiune specifica
DELETE /sessions?userId=...             revoaca TOATE sesiunile unui user
```

#### Model
```
Session {
  sessionId:   UUID
  userId:      ref catre user
  deviceId:    fingerprint dispozitiv (UUID generat de client)
  ipAddress:   ultima IP cunoscuta
  userAgent:   browser/device string
  createdAt:   ISO-8601
  expiresAt:   TTL = 1 ora default
  lastSeenAt:  ultima activitate (actualizat la fiecare request)
  status:      ACTIVE | REVOKED | EXPIRED
}
```

---

### 4.5 gateway-svc — Payment API Entry Point

**Port:** 8084
**Locatie:** `services/gateway-svc/`

#### Responsabilitate
**Orchestratorul central al platilor.** Primeste cererile de la merchant, coordoneaza toate serviciile implicate (vault, fraud, network), publica events Kafka pentru audit si notificari.

#### Tehnologii
- Spring Boot 3.4.4 + Spring Security (JWT validation)
- RestTemplate (HTTP clients pentru vault, fraud, network)
- Spring Kafka (producer pentru payment-events)
- ConcurrentHashMap (in-memory store — suficient pentru demo)

#### Endpoint-uri
```
POST   /v1/payments/authorize           autorizeaza plata
POST   /v1/payments/{txnId}/capture     captureaza autorizare
POST   /v1/payments/{txnId}/refund      ramburseaza captura
GET    /v1/payments/{txnId}             status plata
```

#### Fluxul de autorizare (cod real)
```java
// 1. Tokenizare PAN (PAN nu se stocheaza niciodata in gateway)
String dpan = vaultClient.tokenize(request.pan(), request.expiryDate());

// 2. Fraud scoring
FraudResult fraud = fraudClient.score(dpan, amount, currency, merchantId, clientIp);

// 3. Blocare risc inalt
if (fraud.isBlocked()) {  // score >= 0.8
    return BLOCKED + SHAP explanation;
}

// 4. Challenge 3DS2 risc mediu
if (fraud.needsChallenge()) {  // score >= 0.5
    return CHALLENGE;  // merchant redirecteaza la 3DS
}

// 5. Autorizare prin retea
AuthorizationResult net = networkClient.authorize(dpan, expiry, amount, ...);

// 6. Publicare Kafka → audit-svc + notif-svc + settlement-svc
eventPublisher.publish(new PaymentAuditEvent(...));

// 7. Raspuns merchant
return PaymentResponse { status: AUTHORIZED|DECLINED, isoCode, authCode, fraudScore }
```

#### Fraud thresholds
```
score < 0.5  → ALLOW  (plata normala, merge direct la network)
0.5 ≤ score < 0.8 → CHALLENGE (3DS2 necesar)
score ≥ 0.8  → BLOCK  (respins cu SHAP explanation — GDPR Art.22)
```

#### PaymentStatus enum
```
PENDING → AUTHORIZED → CAPTURED → REFUNDED
PENDING → DECLINED
PENDING → BLOCKED (fraud)
PENDING → CHALLENGE (3DS2 necesar)
PENDING → ERROR (eroare tehnica)
```

---

### 4.6 vault-svc — Tokenizare EMV (PAN → DPAN)

**Port:** 8085
**Locatie:** `services/vault-svc/`

#### Responsabilitate
**Token Vault** conform standardului EMV Payment Tokenization. Converteste numarul real de card (PAN) intr-un token (DPAN) care poate circula prin sisteme fara sa expuna datele reale. PAN-ul real este criptat cu **AES-256-GCM** si stocat in MiniDS.

#### Tehnologii
- AES-256-GCM (Java JCE)
- Algoritmul Luhn (validare numere card)
- MiniDS client (storage tokene)

#### Endpoint-uri
```
POST   /vault/tokenize                  PAN → DPAN
POST   /vault/detokenize/{dpan}         DPAN → PAN (restrictionat)
DELETE /vault/tokens/{dpan}             sterge token (GDPR erasure)
```

#### Algoritmul de tokenizare
```
Input:  PAN (13-19 cifre), ExpiryDate (MM/yy)
Output: DPAN (16 cifre, Luhn-valid)

Pasii:
1. Validare PAN cu algoritmul Luhn
2. Extrage BIN = primele 6 cifre (pastrate in DPAN pentru routing)
3. Genereaza 9 cifre random criptografic sigure
4. Calculeaza cifra de control Luhn
5. DPAN = BIN (6) + random (9) + checksum (1) = 16 cifre
6. Cripteaza PAN cu AES-256-GCM:
   - IV (nonce): 96-bit random generat per-criptare
   - Ciphertext = encrypt(PAN, key, IV)
7. Stocheaza in MiniDS: { dpan, encPan, encExpiry, bin, createdAt }
8. Returneaza DPAN — PAN NICIODATA stocat in plaintext
```

#### AES-256-GCM — De ce
```
AES-256-GCM (Galois/Counter Mode):
  - Autenticat: detecteaza orice modificare a ciphertextului (AEAD)
  - IV unic per-criptare: acelasi PAN criptat de 100x → 100 ciphertexturi diferite
  - Hardware accelerated: Intel AES-NI (< 1ns per operatie)
  
vs AES-CBC (folosit de unele sisteme vechi):
  - CBC nu autentifica → vulnerabil la padding oracle attacks
  - GCM rezolva complet aceasta problema
```

---

### 4.7 network-svc — Simulator Retea Bancara

**Port:** 8086
**Locatie:** `services/network-svc/`

#### Responsabilitate
Simuleaza **nivelul de retea Visa/Mastercard**. Primeste DPAN de la gateway, il detokenizeaza catre PAN real, si trimite cererea de autorizare la banca emitenta (issuer-svc).

#### Endpoint-uri
```
POST   /network/authorize               rutare cerere autorizare catre issuer
```

#### Flux
```
Input: { dpan, amount, currency, merchantId, txnId }

1. Detokenizeaza DPAN → PAN (call vault-svc)
2. Extrage BIN din PAN
3. Roteaza la issuer-svc (simplifcat: un singur issuer)
4. Returneaza raspuns ISO 8583

Real Visa/MC: routing per BIN catre zeci de banci diferite,
              mesaje ISO 8583 full, MAC-uri criptografice
```

#### Coduri ISO 8583
```
"00" → Approved
"05" → Do Not Honor (card blocat / frauda)
"14" → Invalid Card Number
"51" → Insufficient Funds
"54" → Card Expired
"65" → Activity Limit Exceeded
"96" → System Malfunction
```

---

### 4.8 issuer-svc — Simulator Banca Emitenta

**Port:** 8087
**Database:** PostgreSQL (`minipay_issuer`)
**Locatie:** `services/issuer-svc/`

#### Responsabilitate
Simuleaza logica **bancii emitente** (banca care a emis cardul). Verifica validitatea cardului, soldul, limita zilnica si autorizeaza sau refuza tranzactia.

#### Tehnologii
- Spring Data JPA + PostgreSQL 16
- Hibernate (ddl-auto: update)

#### Endpoint-uri
```
POST   /issuer/authorize                proceseaza autorizare
GET    /issuer/cards                    listeaza carduri test (debug)
```

#### Verificari de autorizare (in ordine)
```java
1. Exista cardul in baza de date?          → daca nu: "14" INVALID_CARD
2. Card ACTIVE / BLOCKED / EXPIRED?        → daca BLOCKED: "05"
3. Data expirare valida?                   → daca expirat: "54"
4. Sold suficient? (amount <= balance)     → daca nu: "51"
5. Limita zilnica? (spent+amount <= limit) → daca nu: "65"
6. Toate OK → deduce suma → "00" APPROVED
```

#### Model Card
```java
@Entity class CardAccount {
  String pan;               // PRIMARY KEY (16 cifre)
  String holderName;
  String expiryDate;        // MM/yy
  CardStatus status;        // ACTIVE | BLOCKED | EXPIRED
  Long balanceInCents;      // Sold curent in bani
  Long dailyLimitInCents;   // Limita zilnica (default: 10.000 USD)
  Long dailySpentInCents;   // Cat s-a cheltuit azi
  LocalDate lastSpentDate;  // Pentru reset zilnic
}
```

#### Carduri de test preincarcate
```
4111111111111111 → VISA, ACTIVE, sold $5,000
4000000000000002 → VISA, BLOCKED (DO_NOT_HONOR)
4000000000009995 → VISA, ACTIVE, sold $0.50 (INSUFFICIENT FUNDS)
5555222222222222 → MASTERCARD, ACTIVE, sold $10,000
```

---

### 4.9 fraud-svc — Machine Learning Fraud Detection

**Port:** 8090
**Locatie:** `services/fraud-svc/`
**Runtime:** Python 3.12 (singurul serviciu non-Java)

#### Responsabilitate
Detecteaza tranzactii frauduloase folosind **XGBoost** (Gradient Boosting) si explica deciziile cu **SHAP** (SHapley Additive exPlanations). Conformitate **GDPR Articolul 22** (dreptul la explicatie pentru decizii automate).

#### Tehnologii
| Librarie | Versiune | Rol |
|---|---|---|
| FastAPI | 0.111.0 | Framework REST |
| XGBoost | 2.0.3 | ML model (Gradient Boosting) |
| SHAP | 0.45.0 | Explainability (TreeExplainer) |
| scikit-learn | 1.5.0 | Preprocessing, metrics |
| pandas | 2.2.2 | Feature engineering |
| Pydantic | 2.7.0 | Schema validation |
| prometheus-client | 0.20.0 | Metrics |

#### Endpoint-uri
```
POST   /fraud/score                     scor tranzactie (0.0 – 1.0)
GET    /health                          status serviciu (UP/LOADING)
GET    /metrics                         Prometheus metrics
```

#### Request / Response
```python
# Request
{
  "dpan": "411111XXXXX1111",
  "amount": 150000,           # centimi (1500 RON)
  "currency": "RON",
  "merchantId": "MERCHANT_01",
  "ipAddress": "87.120.45.12"
}

# Response
{
  "score": 0.87,              # probabilitate frauda (0-1)
  "decision": "BLOCK",        # ALLOW | CHALLENGE | BLOCK
  "reasons": [
    "Suma tranzactie neobisnuit de mare",
    "Viteza geografica ridicata (IP nou)",
    "Merchant categorie risc inalt"
  ],
  "shap_details": [
    { "feature": "amount", "shap_value": 0.34, "description": "Suma 1500 RON >> medie 420 RON" },
    { "feature": "ip_velocity", "shap_value": 0.28, "description": "IP nou, 2 tari in 30 min" },
    { "feature": "merchant_risk", "shap_value": 0.21, "description": "Categorie bijuterii/luxury" }
  ]
}
```

#### SHAP Explainability — De ce conteaza
```
XGBoost fara SHAP = "black box" — stii doar scorul, nu de ce

Cu SHAP TreeExplainer:
  - Calculeaza contributia Shapley a fiecarei feature la decizie
  - Sortare dupa |shap_value| → top 3 motive principale
  - Human-readable descriptions (in romana)

GDPR Art.22: "dreptul de a nu fi supus unor decizii exclusiv automate"
  → MiniPay explica fiecare decizie BLOCK/CHALLENGE
  → Stripe Radar este black-box (nu ofera explicatii GDPR-compliant)

Aceasta este o contributie originala fata de sistemele comerciale.
```

#### Antrenare model
```python
# Date sintetice: 8500 tranzactii legitime + 1500 frauduloase
# Features:
#   - amount (suma in centimi)
#   - hour_of_day (0-23, risc crescut noaptea)
#   - merchant_risk_score (0-1, categorie merchant)
#   - card_velocity (tranzactii/ora pe acelasi card)
#   - ip_velocity (tranzactii/ora de pe acelasi IP)
#   - amount_vs_avg_ratio (suma / media istorica utilizator)
#   - is_foreign_ip (0/1, IP din alta tara)
#   - is_new_merchant (0/1, merchant necunoscut)

# Metrici model (test set):
#   Accuracy: ~94%
#   Precision fraud: ~89%
#   Recall fraud: ~87%
#   AUC-ROC: ~0.97
```

---

### 4.10 audit-svc — Merkle Tree Audit Log Imutabil

**Port:** 8091
**Database:** PostgreSQL (`minipay_audit`)
**Locatie:** `services/audit-svc/`

#### Responsabilitate
**Audit log imutabil** bazat pe un **SHA-256 hash chain** (inspirat din principiul Merkle Tree). Orice alterare a unui entry din log este detectabila instant. Conformitate **PCI DSS Requirement 10** (audit log nemodificabil).

#### Tehnologii
- Spring Boot 3.4.4 + Spring Data JPA
- PostgreSQL 16
- Apache Kafka (consumer `audit-svc-group`)
- SHA-256 (java.security.MessageDigest)

#### Endpoint-uri
```
GET    /audit/entries                   log paginat (newest first)
GET    /audit/entries/{txnId}           entry dupa txnId
GET    /audit/verify                    verifica integritatea intregului lant
POST   /audit/tamper-demo/{txnId}       DEMO: simuleaza alterare entry
```

#### Algoritmul Hash Chain
```
Genesis entry: prevHash = "0000000000000000...0000" (64 zerouri)

Pentru fiecare entry nou:
  1. Preia prevHash din ultimul entry din DB
  2. sequenceNumber = count + 1
  3. entryHash = SHA-256(
       sequenceNumber
       + txnId + status + amount + currency
       + merchantId + fraudScore + eventTimestamp
       + prevHash                              ← asta leaga lantul!
     )
  4. Salveaza: { seq, txnId, ..., prevHash, entryHash }
  5. La urmatorul entry: prevHash = acest entryHash

Verificare integritate:
  Pentru fiecare entry in ordine:
    computed = SHA-256(seq + txnId + ... + prevHash)
    if computed != entryHash → TAMPERED! (modificat retroactiv)
    prevHash = entryHash (continua lantul)

Demo prezentare disertatie:
  GET /audit/verify → { isValid: true, chainLength: 47 }
  POST /audit/tamper-demo/txn-123 → modifica suma in DB
  GET /audit/verify → { isValid: false, firstTamperedAt: 23, txnId: "txn-123" }
```

#### Model AuditEntry
```java
@Entity class AuditEntry {
  Long   id;
  Long   sequenceNumber;    // Ordine in lant (imutabila)
  String txnId;
  String status;            // AUTHORIZED / DECLINED / BLOCKED / etc.
  Long   amount;
  String currency;
  String merchantId;
  Double fraudScore;
  String eventTimestamp;
  String prevHash;          // SHA-256 al entry-ului anterior
  String entryHash;         // SHA-256 al acestui entry + prevHash
}
```

#### De ce este relevant pentru disertatie
```
Sistemele de audit traditionale:
  → Log files: se pot modifica cu editor text
  → Database: UPDATE/DELETE sterg urmele

Hash Chain MiniPay:
  → Orice modificare retroactiva rupe lantul
  → Detectare instantanee prin /audit/verify
  → Similar blockchain dar centralizat (mai eficient)
  → PCI DSS Req.10: "implementati piste de audit care nu pot fi distruse"
```

---

### 4.11 tds-svc — 3D Secure 2.0 Authentication

**Port:** 8088 (configurat 8096 in K8s)
**Database:** PostgreSQL
**Locatie:** `services/tds-svc/`

#### Responsabilitate
Simuleaza un **ACS (Access Control Server) 3DS2**. Emite challenge-uri OTP pentru tranzactii cu risc mediu (fraud score 0.5–0.8). Verifica OTP-ul introdus de utilizator.

#### Endpoint-uri
```
POST   /3ds/authenticate                initiaza challenge 3DS2
GET    /3ds/challenge/{acsTransId}      citeste sesiunea de challenge
POST   /3ds/challenge/{acsTransId}      trimite OTP pentru verificare
```

#### Flow 3DS2
```
1. gateway-svc detecteaza fraud.score ∈ [0.5, 0.8)
2. gateway-svc returneaza status=CHALLENGE + acsTransId
3. Merchant redirecteaza user la /3ds/challenge/{acsTransId}
4. tds-svc: genereaza OTP 6 cifre, expira in 5 minute
5. User introduce OTP
6. tds-svc valideaza OTP → status=AUTHENTICATED
7. gateway-svc continua autorizarea
```

#### Model
```java
ChallengeSession {
  acsTransID:  UUID
  status:      CHALLENGE_REQUIRED | AUTHENTICATED | EXPIRED | FAILED
  otp:         6 cifre (demo: expus in GET pentru testare)
  expiresAt:   Instant (5 minute TTL)
  attempts:    int (max 3 incercari)
}
```

---

### 4.12 settlement-svc — Clearing Zilnic

**Port:** 8094
**Database:** PostgreSQL (`minipay_settlement`)
**Locatie:** `services/settlement-svc/`

#### Responsabilitate
Genereaza **batches de clearing zilnic** pentru merchant settlement. Consuma events din Kafka si agregeaza tranzactiile per merchant per zi. Reconciliere automata la ora 01:00 UTC.

#### Endpoint-uri
```
GET    /settlements/batches             batches pentru interval de date
GET    /settlements/batches/merchant/{id}  batches per merchant
GET    /settlements/records             inregistrari individuale
POST   /settlements/reconcile           reconciliere on-demand
```

#### Model
```java
SettlementBatch {
  batchId:          UUID
  settlementDate:   LocalDate
  merchantId:       String
  transactionCount: Long
  totalAmount:      BigDecimal
  fees:             BigDecimal (0.3% default)
  netAmount:        BigDecimal (totalAmount - fees)
  status:           PENDING | SETTLED | RECONCILED
}
```

---

### 4.13 psd2-svc — Open Banking PSD2

**Port:** 8095
**Locatie:** `services/psd2-svc/`

#### Responsabilitate
Implementeaza **PSD2 Directive (EU 2015/2366)** pentru Open Banking:
- **AIS** (Account Information Service) — balante si tranzactii
- **PIS** (Payment Initiation Service) — initiere plati SEPA
- **Consent Management** — acces bazat pe consimtamant

#### Endpoint-uri (AIS)
```
POST   /psd2/consents                   creeaza consent (AIS/PIS)
GET    /psd2/consents/{consentId}       status consent
DELETE /psd2/consents/{consentId}       revoaca consent
GET    /psd2/accounts                   listeaza conturi (Consent-ID header)
GET    /psd2/accounts/{id}/balances     sold cont
GET    /psd2/accounts/{id}/transactions tranzactii cont
POST   /psd2/payments/sepa-credit-transfers  initiere plata SEPA (PIS)
```

---

### 4.14 notif-svc — Notificari

**Port:** 8093
**Locatie:** `services/notif-svc/`

#### Responsabilitate
Consuma events Kafka si genereaza notificari pentru utilizatori (email, push, SMS). Rutare pe canal in functie de scorul de frauda.

#### Endpoint-uri
```
GET    /notifications                   ultimele 500 notificari
GET    /notifications/{txnId}           notificari pentru tranzactie
GET    /notifications/stats             statistici pe canal si status
```

#### Logica rutare
```
fraudScore < 0.3  → EMAIL (tranzactie normala)
0.3 ≤ score < 0.7 → EMAIL + PUSH
score ≥ 0.7       → EMAIL + PUSH + SMS (alert frauda)
```

---

### 4.15 Frontend — Next.js Dashboard

**Locatie:** `frontend/`
**Deploy:** Vercel
**URL:** (in configurare)

#### Stack
| Tehnologie | Versiune | Rol |
|---|---|---|
| Next.js | 14 | Framework React (SSR + SSG) |
| React | 18 | UI library |
| TypeScript | 5.x | Type safety |
| Tailwind CSS | 3.x | Styling utility-first |
| shadcn/ui | latest | Componente UI |
| Axios | latest | HTTP client |
| Zustand | latest | State management |

#### Pagini dashboard
```
/login                  → autentificare OAuth2
/dashboard              → overview plati (stats, grafice)
/transactions           → lista tranzactii cu filtre
/users                  → management utilizatori
/vault                  → tokene active
/fraud                  → analytics fraud + SHAP
/audit                  → audit log cu verificare hash chain
/settlements            → batches clearing
/notifications          → notificari in timp real
/psd2                   → conturi Open Banking
/tds                    → sesiuni 3DS2
/shop                   → demo merchant (checkout E2E)
```

#### Proxy Next.js (eliminare CORS)
```typescript
// frontend/src/app/api/proxy/[...path]/route.ts
// Toate cererile /api/* sunt proxiate catre api-minipay.online
// Elimina problemele CORS pentru deployment Vercel → DigitalOcean
```

---

## 5. Fluxul Complet al unei Plati (E2E)

```
1.  User deschide /shop → selecteaza produs → checkout

2.  POST /v1/payments/authorize
      { pan: "4111111111111111", expiry: "12/27", amount: 15000, ... }

3.  gateway-svc primeste requestul (autentificat cu JWT)

4.  vault-svc:   PAN → DPAN
      Algoritm: BIN(6) + random(9) + Luhn(1) = 16 cifre
      AES-256-GCM cripteaza PAN in MiniDS
      Return: "411111XXXXX1234"  ← DPAN

5.  fraud-svc:   XGBoost score
      Return: { score: 0.12, decision: "ALLOW", reasons: [...] }

6.  (score < 0.5 → skip 3DS)

7.  network-svc: autorizeaza
      Detokenizeaza DPAN → PAN via vault-svc
      Trimite PAN la issuer-svc

8.  issuer-svc:  verifica card + sold
      balance: 500000 centimi (5000 RON)
      amount:  15000 centimi (150 RON)
      → "00" APPROVED, authCode="DEA161"

9.  gateway-svc: publica Kafka event
      → audit-svc: append in hash chain, seq=48
      → notif-svc: genereaza EMAIL notificare
      → settlement-svc: adauga in batch zilnic

10. Response catre merchant/user:
      {
        "txnId": "550e8400-e29b-41d4-a716-446655440000",
        "status": "AUTHORIZED",
        "isoResponseCode": "00",
        "authCode": "DEA161",
        "fraudScore": 0.12,
        "amount": 15000,
        "currency": "RON",
        "timestamp": "2026-05-11T14:30:00Z"
      }

Timp total: ~180ms
```

---

## 6. Infrastructura si Deployment

### 6.1 DigitalOcean Droplet (Backend Live)

```
Spec: 4 vCPU / 8GB RAM / Frankfurt (fra1)
OS:   Ubuntu 22.04 LTS
K8s:  k3s v1.34+ (single-node control-plane)

Domeniu: api-minipay.online (GoDaddy DNS → IP Droplet)
TLS:     Traefik + Let's Encrypt (auto-renewal)

Namespace: minipay

RAM allocation:
  Requests: ~3.6 GB
  Limits:   ~7.4 GB
  → Incape confortabil in 8GB
```

### 6.2 Kubernetes — Structura manifests

```
k8s/
├── namespace.yaml                  # Namespace + ResourceQuota
├── secrets.yaml                    # DB passwords, JWT keys, Kafka creds
├── ingress.yaml                    # Traefik routing (path-based)
├── traefik-config.yaml             # TLS + middleware
├── data/
│   └── minids.yaml                 # StatefulSet 3 noduri + headless Service
├── infra/
│   ├── postgres.yaml               # StatefulSet PostgreSQL 16
│   ├── kafka.yaml                  # StatefulSet Kafka KRaft (no Zookeeper)
│   └── kafka-ui.yaml               # Deployment Kafka UI
├── identity/
│   ├── auth-svc.yaml
│   ├── user-svc.yaml
│   └── session-svc.yaml
├── payments/
│   ├── gateway-svc.yaml
│   ├── vault-svc.yaml
│   ├── network-svc.yaml
│   ├── issuer-svc.yaml
│   ├── settlement-svc.yaml
│   └── psd2-svc.yaml
├── security/
│   ├── fraud-svc.yaml
│   ├── audit-svc.yaml
│   ├── notif-svc.yaml
│   └── tds-svc.yaml
└── monitoring/
    ├── prometheus.yaml
    └── grafana.yaml
```

### 6.3 Ingress Routing (Traefik)

```
api-minipay.online/gateway/*    → gateway-svc:8084
api-minipay.online/auth/*       → auth-svc:8081
api-minipay.online/users/*      → user-svc:8082
api-minipay.online/sessions/*   → session-svc:8083
api-minipay.online/vault/*      → vault-svc:8085
api-minipay.online/network/*    → network-svc:8086
api-minipay.online/issuer/*     → issuer-svc:8087
api-minipay.online/fraud/*      → fraud-svc:8090
api-minipay.online/audit/*      → audit-svc:8091
api-minipay.online/notif/*      → notif-svc:8093
api-minipay.online/tds/*        → tds-svc:8096
api-minipay.online/settlement/* → settlement-svc:8094
api-minipay.online/psd2/*       → psd2-svc:8095
api-minipay.online/minids/*     → minids:8311
```

### 6.4 CI/CD Pipeline — GitHub Actions

**Fisier:** `.github/workflows/ci-cd.yml`

```
Trigger: git push → main

Step 1 — Build
  Matrix (14 jobs paralele):
    - Maven build fiecare serviciu Java (cache ~/.m2)
    - pip install + test fraud-svc Python

Step 2 — Docker Build & Push
  14 imagini paralele → ghcr.io/arobertm/minipay/<service>:<sha>
  Registry: GitHub Container Registry (gratuit, public)

Step 3 — Update Manifests
  sed image tags in k8s/*.yaml cu noul SHA
  git commit + push → triggereaza Argo CD

Step 4 — Argo CD (pe Droplet)
  Auto-sync la fiecare commit pe main
  Rolling update fara downtime
  kubectl rollout status pana la ready

Total durata: ~12 minute (prima data), ~7 minute (cu cache)
```

### 6.5 MiniDS pe Kubernetes (StatefulSet)

```yaml
# Headless Service pentru DNS stabil:
minids-0.minids.minipay.svc.cluster.local → minids-0
minids-1.minids.minipay.svc.cluster.local → minids-1
minids-2.minids.minipay.svc.cluster.local → minids-2

# PVC per pod: 2Gi local-path (pe nodul k3s)
# Raft transport: HTTP pe portul API (8301) via /raft/message
# Nu necesita port separat pentru Raft (simplificare arhitecturala)
```

### 6.6 Monitoring

```
Prometheus (port 9090):
  - Scrape toate serviciile via /actuator/prometheus
  - Metrici: request latency, error rate, fraud scores, Kafka lag

Grafana (port 3000):
  - Dashboard payment funnel (authorize → capture → settle)
  - Dashboard fraud (scor distributie, block rate)
  - Dashboard infrastructure (CPU, RAM, Kafka lag)

Kafka UI (port 8080):
  - Vizualizare topics si messages in timp real
```

---

## 7. Stack Tehnologic Complet

### Backend Java (12 servicii)
| Tehnologie | Versiune | Utilizare |
|---|---|---|
| Java | 21 (LTS) | Runtime, Project Loom Virtual Threads |
| Spring Boot | 3.4.4 | Framework aplicatii |
| Spring Authorization Server | 1.3.2 | OAuth2/OIDC |
| Spring Security | 6.x | Autentificare/autorizare |
| Spring Data JPA | 3.x | ORM pentru PostgreSQL |
| Spring Kafka | 3.x | Producer/Consumer Kafka |
| Bouncy Castle | 1.78.1 | PQC (CRYSTALS-Dilithium3) |
| MicroRaft | 0.7 | Consensus Raft |
| RocksDB | 9.0.0 | Key-Value embedded storage |
| Lombok | latest | Reducere boilerplate |
| Hibernate | 6.x | ORM |

### Backend Python (1 serviciu)
| Tehnologie | Versiune | Utilizare |
|---|---|---|
| Python | 3.12 | Runtime |
| FastAPI | 0.111.0 | Framework REST async |
| XGBoost | 2.0.3 | ML Gradient Boosting |
| SHAP | 0.45.0 | Explainability |
| scikit-learn | 1.5.0 | Preprocessing, metrics |
| pandas | 2.2.2 | Feature engineering |
| Pydantic | 2.7.0 | Validare schema |
| prometheus-client | 0.20.0 | Metrici |

### Infrastructure
| Tehnologie | Versiune | Utilizare |
|---|---|---|
| PostgreSQL | 16 | Baze de date relationale |
| Apache Kafka | 3.7 (KRaft) | Message streaming |
| Kubernetes (k3s) | latest | Orchestrare containere |
| Docker | latest | Containerizare |
| Traefik | v2 | Ingress + TLS |
| Argo CD | latest | GitOps CD |
| GitHub Actions | latest | CI |
| Prometheus | latest | Metrici |
| Grafana | latest | Vizualizare |

### Frontend
| Tehnologie | Versiune | Utilizare |
|---|---|---|
| Next.js | 14 | Framework React |
| React | 18 | UI |
| TypeScript | 5 | Type safety |
| Tailwind CSS | 3 | Styling |
| shadcn/ui | latest | Componente |

---

## 8. Contributii Originale (pentru disertatie)

| # | Contributie | Ce implementeaza | De ce este original |
|---|---|---|---|
| 1 | **Post-Quantum Cryptography** | JWT semnat CRYSTALS-Dilithium3 (NIST FIPS 204) in auth-svc | Stripe/Google Pay folosesc RSA/ECDSA — vulnerabile la quantum computing; MiniPay e pregatit |
| 2 | **Fraud XAI (SHAP)** | XGBoost + SHAP TreeExplainer in fraud-svc | Stripe Radar e black-box; MiniPay explica fiecare decizie (GDPR Art.22 compliant) |
| 3 | **Hash Chain Audit Log** | SHA-256 chain imutabila in audit-svc | Orice alterare retroactiva a logului este detectabila instant (PCI DSS Req.10) |
| 4 | **MiniDS (Raft + RocksDB)** | Directory server distribuit cu MicroRaft + RocksDB | Arhitectura transparenta si documentata (similar etcd/CockroachDB) — nu black-box |
| 5 | **PSD2 Open Banking** | AIS + PIS complet in psd2-svc | Netopia nu are; Stripe are partial; MiniPay implementeaza standardul EU complet |

---

## 9. Scenarii Demo Prezentare

### Scenariul 1 — Plata normala (happy path)
```bash
POST /v1/payments/authorize
  pan: "4111111111111111", amount: 15000, currency: "RON"

Rezultat asteptat:
  status: AUTHORIZED, iso: "00", authCode: "DEA161", fraudScore: ~0.12
Timp: ~180ms
```

### Scenariul 2 — Frauda detectata (BLOCK + SHAP)
```bash
POST /v1/payments/authorize
  pan: "4111111111111111", amount: 9999900  # ~100.000 RON

Rezultat asteptat:
  status: BLOCKED, fraudScore: ~0.87
  reasons: ["Suma extrem de mare", "Depaseste limita istorica", ...]
  # GDPR Art.22 — explicatie completa furnizata
```

### Scenariul 3 — Card blocat (DO_NOT_HONOR)
```bash
POST /v1/payments/authorize
  pan: "4000000000000002"  # BLOCKED card

Rezultat asteptat:
  status: DECLINED, iso: "05", reason: DO_NOT_HONOR
```

### Scenariul 4 — Fond insuficient
```bash
POST /v1/payments/authorize
  pan: "4000000000009995", amount: 10000  # sold $0.50

Rezultat asteptat:
  status: DECLINED, iso: "51", reason: INSUFFICIENT_FUNDS
```

### Scenariul 5 — Audit tamper detection
```bash
# Verifica lant (intact)
GET /audit/verify
→ { isValid: true, chainLength: 47 }

# Simuleaza alterare in PostgreSQL
POST /audit/tamper-demo/txn-abc123

# Verifica din nou (detecteaza alterarea)
GET /audit/verify
→ { isValid: false, firstTamperedAt: 23, txnId: "txn-abc123" }
```

### Scenariul 6 — MiniDS failover Raft
```bash
# Pe Droplet:
kubectl delete pod minids-0 -n minipay   # opreste Leader
# minids-1 sau minids-2 devine nou Leader in ~300ms
# Platile continua fara intrerupere
kubectl get pods -n minipay              # minids-0 restartat automat
```

### Scenariul 7 — JWT Post-Quantum
```bash
GET /oauth2/jwks
# Raspuns contine DOUA chei:
# 1. RSA-2048 (standard)
# 2. Dilithium3 (NIST FIPS 204, post-quantum)

POST /auth/token/pqc
# Returneaza JWT semnat cu Dilithium3
# Header: { alg: "DILITHIUM3" }
# Semnatura: 3293 bytes (vs 256 bytes RSA)
```

---

## 10. Testare

### Carduri de test
```
VISA APPROVED:          4111 1111 1111 1111
VISA DECLINED (blocked):4000 0000 0000 0002
VISA INSUF. FUNDS:      4000 0000 0000 9995
MASTERCARD APPROVED:    5555 2222 2222 2222
CVV: orice 3 cifre | Expiry: orice data viitoare (ex: 12/27)
```

### Response Codes ISO 8583
```
"00" → Approved
"05" → Do Not Honor (card blocat / frauda)
"14" → Invalid Card Number
"51" → Insufficient Funds
"54" → Expired Card
"65" → Activity Limit Exceeded
"96" → System Malfunction
```

### Test E2E (Python pytest)
```
e2e/
├── conftest.py                    # Setup + fixtures
├── test_01_health.py              # Health checks toate serviciile
├── test_02_auth.py                # OAuth2 token flow
├── test_03_payment_happy_path.py  # Plata normala E2E
├── test_04_payment_declined.py    # Carduri blocate / fonduri
├── test_05_fraud.py               # Fraud score + SHAP
├── test_06_vault.py               # Tokenizare / detokenizare
├── test_07_audit.py               # Hash chain + tamper detection
├── test_08_tds.py                 # 3DS2 challenge flow
├── test_09_settlement_psd2.py     # Settlement + Open Banking
└── test_10_notifications.py       # Kafka → notificari
```

---

## 11. Surse si Bibliografie

### Standarde Oficiale
| # | Sursa | Relevanta |
|---|---|---|
| 1 | PCI DSS v4.0 (2022) | Req.3 (tokenizare), Req.10 (audit log) |
| 2 | EMV Tokenization Spec | PAN → DPAN, DPAN format |
| 3 | NIST FIPS 204 — ML-DSA (Dilithium) | auth-svc PQC |
| 4 | NIST FIPS 203 — ML-KEM (Kyber) | Directii viitoare |
| 5 | PSD2 Directive EU 2015/2366 | psd2-svc |
| 6 | GDPR EU 2016/679, Art.22 | fraud-svc SHAP |
| 7 | RFC 6749 — OAuth2 | auth-svc |
| 8 | RFC 7636 — PKCE | auth-svc |
| 9 | ISO 8583 — Financial Messages | network-svc, issuer-svc |

### Algoritmi si Protocoale
| # | Sursa | Relevanta |
|---|---|---|
| 1 | Ongaro & Ousterhout, "Raft: In Search of an Understandable Consensus Algorithm" (2014) | MiniDS |
| 2 | Ducas et al., "CRYSTALS-Dilithium: A Lattice-Based Digital Signature Scheme" (2018) | auth-svc PQC |
| 3 | Lundberg, "A Unified Approach to Interpreting Model Predictions" (SHAP, 2017) | fraud-svc |
| 4 | Chen & Guestrin, "XGBoost: A Scalable Tree Boosting System" (2016) | fraud-svc |

### Carti
| # | Carte | Autor | An |
|---|---|---|---|
| 1 | Security Engineering (ed. 3) | Ross Anderson | 2020 |
| 2 | Applied Cryptography | Bruce Schneier | clasic |
| 3 | Designing Distributed Systems | Brendan Burns | 2018 |
| 4 | Microservices Patterns | Chris Richardson | 2018 |

### Rapoarte Industrie
- McKinsey Global Payments Report 2023/2024
- ECB — Digital Euro Reports (ecb.europa.eu)
- BNR — Rapoarte sisteme de plata Romania (bnr.ro)
- Stripe Engineering Blog (stripe.com/blog/engineering)

---

## 12. Dependinte Maven Principale

```xml
<!-- Spring Boot Parent -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.4</version>
</parent>

<!-- OAuth2 Authorization Server -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-authorization-server</artifactId>
    <version>1.3.2</version>
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

<!-- MicroRaft (Raft consensus) -->
<dependency>
    <groupId>io.microraft</groupId>
    <artifactId>microraft</artifactId>
    <version>0.7</version>
</dependency>

<!-- RocksDB (embedded KV store) -->
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

<!-- Spring Data JPA + PostgreSQL -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

*Documentatie generata: Mai 2026 — Proiect finalizat*
