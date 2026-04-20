# MiniPay Frontend — Plan de Implementare

> **Backend:** `https://api-minipay.online`
> **Hosting:** Vercel
> **Tip aplicație:** Merchant / Developer Dashboard (similar Stripe Dashboard)
> **Scop:** Demonstrație disertație — acoperă toate domeniile implementate în backend

---

## 1. Tech Stack

| Layer | Decizie | Motiv |
|---|---|---|
| Framework | **Next.js 14 (App Router)** | Native Vercel, SSR, file-based routing |
| Limbaj | **TypeScript** | Type safety pe toate API responses |
| Styling | **Tailwind CSS** | Utility-first, rapid |
| Componente | **shadcn/ui** | Profesional, accesibil, zero overhead |
| Data fetching | **TanStack Query v5** | Cache, loading/error automat, refetch |
| Global state | **Zustand** | JWT token + user info |
| Charts | **Recharts** | Fraud scores, volume tranzacții |
| HTTP client | **Axios** (instanță per domeniu) | Interceptor global pentru Bearer token |
| Form handling | **React Hook Form + Zod** | Validare tipizată |

---

## 2. Structura Proiectului

```
frontend/
├── app/
│   ├── layout.tsx                  # Root layout: sidebar + header
│   ├── page.tsx                    # Redirect → /dashboard
│   ├── (auth)/
│   │   └── login/page.tsx          # Login form
│   ├── (dashboard)/
│   │   ├── layout.tsx              # Sidebar navigation
│   │   ├── dashboard/page.tsx      # Overview + stats
│   │   ├── transactions/
│   │   │   ├── page.tsx            # Listă tranzacții
│   │   │   └── [id]/page.tsx       # Detalii tranzacție
│   │   ├── fraud/page.tsx          # Fraud scoring + SHAP
│   │   ├── tds/page.tsx            # 3DS2 challenge demo
│   │   ├── settlements/page.tsx    # Batches + reconcile
│   │   ├── notifications/page.tsx  # Feed notificări
│   │   ├── audit/page.tsx          # Audit log + Merkle verify
│   │   ├── vault/page.tsx          # Tokenize / Detokenize
│   │   ├── psd2/page.tsx           # Open Banking AIS + PIS
│   │   └── users/page.tsx          # User management
├── components/
│   ├── ui/                         # shadcn components
│   ├── layout/
│   │   ├── Sidebar.tsx
│   │   ├── Header.tsx
│   │   └── StatCard.tsx
│   └── features/
│       ├── transactions/
│       │   ├── TransactionTable.tsx
│       │   ├── AuthorizeForm.tsx
│       │   └── CaptureRefundButtons.tsx
│       ├── fraud/
│       │   ├── FraudScoreForm.tsx
│       │   └── ShapChart.tsx
│       ├── tds/
│       │   └── OtpChallengeModal.tsx
│       ├── settlements/
│       │   ├── BatchTable.tsx
│       │   └── ReconcileButton.tsx
│       ├── audit/
│       │   ├── AuditTable.tsx
│       │   └── MerkleVerifyBadge.tsx
│       └── vault/
│           └── TokenizeForm.tsx
├── lib/
│   ├── api/
│   │   ├── axios.ts                # Instanță de bază cu interceptor JWT
│   │   ├── gateway.ts              # POST /authorize, /capture, /refund, GET /payments
│   │   ├── auth.ts                 # POST /oauth2/token
│   │   ├── users.ts                # POST/GET/PUT /users
│   │   ├── fraud.ts                # POST /fraud/score
│   │   ├── tds.ts                  # POST /tds/...
│   │   ├── settlements.ts          # GET /settlements/batches, POST /reconcile
│   │   ├── notifications.ts        # GET /notif/notifications
│   │   ├── audit.ts                # GET /audit/entries, /audit/verify
│   │   ├── vault.ts                # POST /vault/tokenize, /detokenize
│   │   └── psd2.ts                 # GET /psd2/accounts, consents, PIS
│   ├── store/
│   │   └── auth.store.ts           # Zustand: token, userId, logout
│   └── types/
│       └── api.types.ts            # Toate tipurile TypeScript pentru responses
├── hooks/
│   ├── useTransactions.ts
│   ├── useFraudScore.ts
│   └── useSettlements.ts
└── public/
    └── logo.svg
```

---

## 3. Pagini — Descriere Completă

### P1 — Login (`/login`)
**Scop:** Autentificare dashboard

**Flow:**
1. User introduce email + parolă
2. Frontend face `POST https://api-minipay.online/auth/oauth2/token`
   - `grant_type=client_credentials`
   - `client_id=minipay-dashboard`
   - `client_secret=<secret>`
3. Primește `access_token` JWT → salvat în Zustand + localStorage
4. Redirect → `/dashboard`

**Componente:** `LoginForm`, input email/parolă, buton Submit, eroare toast

---

### P2 — Dashboard (`/dashboard`)
**Scop:** Overview general cu metrici live

**Carduri stats:**
- Total tranzacții autorizate (din audit log count)
- Fraud rate % (tranzacții cu `fraudScore > 0.7` / total)
- Settlement pending (din `/settlements/batches` cu status PENDING)
- Notificări necitite (din `/notif/notifications/stats`)

**Chart:** Recharts BarChart — volume tranzacții pe ultimele 7 zile (mock dacă nu există endpoint dedicat)

**API calls:**
```
GET /audit/entries?page=0&size=5       → ultimele 5 events
GET /notif/notifications/stats         → stats
GET /settlements/batches               → batches recente
```

---

### P3 — Transactions (`/transactions`)
**Scop:** Centrul principal — vizualizare și acțiuni pe plăți

**Secțiuni:**
1. **Tabel tranzacții** — ID, amount, status (AUTHORIZED/CAPTURED/REFUNDED), timestamp, fraudScore badge
2. **Form: Authorize Payment** — input PAN (sau DPAN), amount, currency, merchant → `POST /api/v1/payments/authorize`
3. **Acțiuni per rând:** buton Capture → `POST /api/v1/payments/{id}/capture` | buton Refund → `POST /api/v1/payments/{id}/refund`

**Detalii (`/transactions/[id]`):**
- Toate câmpurile tranzacției
- Fraud score + SHAP bars
- Statusul 3DS dacă a fost aplicat

**API calls:**
```
POST /api/v1/payments/authorize
POST /api/v1/payments/{id}/capture
POST /api/v1/payments/{id}/refund
GET  /api/v1/payments/{id}
```

---

### P4 — Fraud Detection (`/fraud`)
**Scop:** Demonstrație XAI — cel mai impresionant pentru disertație

**Layout:**
- Stânga: Form cu câmpuri pentru scoring manual (amount, country, cardType, hour, etc.)
- Dreapta: rezultat cu:
  - **Scor mare colorat** (verde < 0.3, galben 0.3–0.7, roșu > 0.7)
  - **SHAP Bars** (Recharts HorizontalBar) — fiecare feature cu contribuția sa (+ sau -)
  - **Decizie recomandată:** APPROVE / REVIEW / DECLINE
  - **Explicație text:** "Principalii factori: amount ridicat (+0.34), țară risc (+0.21)..."

**API calls:**
```
POST /fraud/score
  body: { amount, currency, merchantCountry, cardType, hour, dayOfWeek, ... }
  response: { fraudScore, decision, shapValues: { feature: value, ... } }
```

---

### P5 — 3DS2 Challenge (`/tds`)
**Scop:** Demo flux 3DS2 — autentificare suplimentară

**Flow vizual în pagină:**
1. Input: `transactionId` + `cardholderEmail`
2. Click "Inițiază 3DS" → `POST /tds/authenticate`
3. Dacă `status = FRICTIONLESS` → badge verde "Approved without challenge"
4. Dacă `status = CHALLENGE` → modal OTP:
   - Input 6 cifre
   - `POST /tds/challenge/verify` cu `{ otp }`
   - Succes → CAVV afișat + badge verde

**API calls:**
```
POST /tds/authenticate
POST /tds/challenge/verify
```

---

### P6 — Settlements (`/settlements`)
**Scop:** Reconciliere financiară

**Secțiuni:**
1. **Tabel Batches** — batchId, date, totalAmount, recordCount, status
2. **Tabel Records** — per tranzacție în batch selectat
3. **Buton "Run Reconciliation"** → `POST /settlements/reconcile` → toast cu rezultat
4. **Cron info:** "Reconciliere automată zilnică la 01:00 UTC"

**API calls:**
```
GET  /settlements/batches
GET  /settlements/records?batchId=...
POST /settlements/reconcile
```

---

### P7 — Notifications (`/notifications`)
**Scop:** Feed de notificări generate de sistem

**Layout:**
- Listă carduri: icon tip (SMS/EMAIL/PUSH), txnId, mesaj, timestamp, badge fraudScore
- Filter: ALL / SMS / EMAIL / PUSH
- Auto-refresh la 10 secunde (TanStack Query `refetchInterval`)

**API calls:**
```
GET /notif/notifications
GET /notif/notifications/stats
GET /notif/notifications/{txnId}
```

---

### P8 — Audit Log (`/audit`)
**Scop:** Demonstrație audit imutabil cu Merkle Tree — punct cheie în disertație

**Secțiuni:**
1. **Tabel entries** — entryId, txnId, action, timestamp, hash
2. **Buton "Verify Merkle Integrity"** → `GET /audit/verify`:
   - Succes → banner verde "✅ Merkle Root valid — lanțul de audit este integru"
   - Eroare → banner roșu "❌ Integrity check FAILED"
3. **Entry detaliat** la click — JSON raw afișat în CodeBlock

**API calls:**
```
GET /audit/entries?page=0&size=20
GET /audit/entries/{txnId}
GET /audit/verify
```

---

### P9 — Token Vault (`/vault`)
**Scop:** Demo tokenizare EMV — PAN → DPAN

**Layout split:**
- **Stânga — Tokenize:**
  - Input PAN (ex: `4111111111111111`), expiry, cvv
  - `POST /vault/tokenize` → afișează DPAN returnat
- **Dreapta — Detokenize:**
  - Input DPAN
  - `POST /vault/detokenize/{dpan}` → afișează PAN original (mascat)
- **Explicație vizuală** sub formular: diagramă simplă PAN → [VAULT AES-256-GCM] → DPAN

**API calls:**
```
POST /vault/tokenize
POST /vault/detokenize/{dpan}
DELETE /vault/tokens/{dpan}
```

---

### P10 — PSD2 Open Banking (`/psd2`)
**Scop:** Demo conformitate PSD2 — AIS + PIS

**Tab 1 — AIS (Account Information):**
- Input `consentId` (sau creare consent)
- Afișează: conturi, sold, ultimele tranzacții pe cont

**Tab 2 — PIS (Payment Initiation):**
- Form SEPA Credit Transfer: IBAN debitor, IBAN creditor, amount, referință
- `POST /psd2/payments/sepa-credit-transfer`
- Afișează `paymentId` + status

**API calls:**
```
POST /psd2/consents
GET  /psd2/accounts?consentId=...
GET  /psd2/accounts/{accountId}/balances
GET  /psd2/accounts/{accountId}/transactions
POST /psd2/payments/sepa-credit-transfer
GET  /psd2/payments/{paymentId}/status
```

---

### P11 — Users (`/users`)
**Scop:** Gestionare utilizatori (demonstrație user-svc)

**Secțiuni:**
1. Form creare user: firstName, lastName, email, parolă, IBAN, PAN
2. Căutare user după email
3. Detalii user + update
4. Schimbare parolă

**API calls:**
```
POST /users/users
GET  /users/users?email=...
GET  /users/users/{id}
PUT  /users/users/{id}
POST /users/users/{id}/change-password
```

---

## 4. Fluxul E2E Principal (Demo Disertație)

Acesta este scenariul complet care traversează toate paginile și demonstrează întregul sistem:

```
STEP 1 — /login
  → Autentificare cu client_credentials
  → JWT salvat în state

STEP 2 — /users
  → Creare user nou: "Ion Popescu", ion@test.ro, IBAN RO49AAAA...
  → userId salvat

STEP 3 — /vault
  → Tokenize PAN 4111111111111111 → DPAN 4999...
  → DPAN copiat pentru pasul următor

STEP 4 — /transactions
  → Authorize payment: DPAN din vault, amount=250.00 EUR
  → Backend: gateway → network → issuer → răspuns AUTHORIZED
  → txnId salvat, fraudScore vizibil în tabel

STEP 5 — /fraud
  → Input manual cu aceleași câmpuri (sau link din tranzacție)
  → SHAP chart afișat: "amount: +0.12, country: +0.08, ..."
  → Decizie: APPROVE (fraudScore < 0.3)

STEP 6 — /tds  [dacă fraudScore >= 0.7]
  → Inițiază 3DS cu txnId
  → OTP modal → introduce cod → CAVV generat

STEP 7 — /transactions
  → Capture pe tranzacția autorizată
  → Status devine CAPTURED

STEP 8 — /settlements
  → Tranzacția apare în records
  → Click "Run Reconciliation" → batch creat

STEP 9 — /notifications
  → Notificare generată: "Payment captured - 250.00 EUR"
  → Channel: EMAIL (fraudScore mic)

STEP 10 — /audit
  → Toate evenimentele apar în log
  → Click "Verify Merkle Integrity" → ✅ VALID

STEP 11 — /psd2
  → AIS: verifică soldul contului IBAN din step 2
  → PIS: inițiază plată SEPA de retur
```

---

## 5. Navigație Sidebar

```
MiniPay Logo
─────────────────────
📊  Dashboard
💳  Transactions
🛡️  Fraud Detection
🔐  3DS2 Challenge
─────────────────────
🏦  Settlements
🔔  Notifications
📋  Audit Log
─────────────────────
🔑  Token Vault
🌐  PSD2 Open Banking
👤  Users
─────────────────────
[avatar] Logout
```

---

## 6. Design System

- **Temă:** Dark (fundal `#0f1117`, card `#1a1d27`) — ca Stripe / Vercel dashboard
- **Accent:** Albastru `#3b82f6` pentru acțiuni principale
- **Status colors:**
  - AUTHORIZED → galben `#f59e0b`
  - CAPTURED → verde `#10b981`
  - REFUNDED → mov `#8b5cf6`
  - FAILED → roșu `#ef4444`
- **Fraud score:**
  - 0.0–0.3 → verde (LOW)
  - 0.3–0.7 → galben (MEDIUM)
  - 0.7–1.0 → roșu (HIGH)
- **Font:** Inter (default Next.js)

---

## 7. Autentificare — Decizie Finală

Auth-svc folosește Spring Authorization Server cu endpoint intern `http://auth-svc:8081`. Extern prin ingress la `https://api-minipay.online/auth/...`.

**Strategie pentru dashboard:**

```
POST https://api-minipay.online/auth/oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials
&client_id=minipay-dashboard
&client_secret=minipay-dashboard-secret
&scope=openid
```

Dacă auth-svc nu are `minipay-dashboard` înregistrat → adăugăm un client hardcodat în `AuthSecurityConfig.java` (InMemoryRegisteredClientRepository temporar).

Token-ul JWT primit se pune în `Authorization: Bearer <token>` pe toate requesturile. Zustand + `localStorage` pentru persistență între refresh-uri.

---

## 8. Ordine de Implementare

| # | Task | Pagini | Prioritate |
|---|---|---|---|
| 1 | Setup Next.js + Tailwind + shadcn + Axios base | — | 🔴 Blocker |
| 2 | Auth store (Zustand) + Login page | P1 | 🔴 Blocker |
| 3 | Layout: Sidebar + Header + routing | toate | 🔴 Blocker |
| 4 | Transactions page (authorize/capture/refund) | P3 | 🔴 Core |
| 5 | Dashboard stats | P2 | 🟡 Important |
| 6 | Fraud page + SHAP chart | P4 | 🔴 Core (disertație) |
| 7 | Audit log + Merkle verify | P8 | 🔴 Core (disertație) |
| 8 | Settlements + reconcile | P6 | 🟡 Important |
| 9 | Notifications feed | P7 | 🟡 Important |
| 10 | Token Vault | P9 | 🟢 Demo |
| 11 | 3DS2 Challenge | P5 | 🟢 Demo |
| 12 | PSD2 Open Banking | P10 | 🟢 Demo |
| 13 | Users page | P11 | 🟢 Demo |

**Total: 11 pagini + 1 layout shared**

---

## 9. Variabile de Mediu Vercel

```env
NEXT_PUBLIC_API_URL=https://api-minipay.online
NEXT_PUBLIC_AUTH_CLIENT_ID=minipay-dashboard
AUTH_CLIENT_SECRET=minipay-dashboard-secret   # server-side only
```

---

## 10. CORS — Acțiune necesară pe backend

Traefik / gateway-svc trebuie să permită requesturi de la domeniul Vercel.

De adăugat în `k8s/ingress.yaml` sau în fiecare serviciu:
```
Access-Control-Allow-Origin: https://minipay.vercel.app (sau domeniu custom)
Access-Control-Allow-Headers: Authorization, Content-Type
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
```
