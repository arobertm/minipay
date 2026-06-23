"""
Seed script — generează plăți de test pentru a popula dashboard-ul.
Rulare: python seed_payments.py [--count 30]
"""
import argparse
import random
import time
import requests

BASE_URL      = "https://api-minipay.online"
CLIENT_ID     = "demo-client"
CLIENT_SECRET = "demo-secret"

# Carduri normale — aprobate sau refuzate de issuer
CARDS_NORMAL = [
    {"pan": "4111111111111111", "expiry": "12/28", "cvv": "123", "label": "VISA approved"},
    {"pan": "5500000000000004", "expiry": "12/28", "cvv": "123", "label": "MC approved"},
    {"pan": "4000000000009995", "expiry": "12/28", "cvv": "456", "label": "VISA insuf funds"},
]

# Scenarii cu risc de fraudă ridicat
# Fraud score depinde de: sumă mare (>2000 RON), valută străină, IP extern
FRAUD_SCENARIOS = [
    # Sumă foarte mare (>200,000 cenți = >2000 RON) → amount_large=1, score ridicat
    {"pan": "4111111111111111", "expiry": "12/28", "cvv": "123",
     "amount": 350000, "currency": "RON", "label": "Sumă mare RON"},
    {"pan": "4111111111111111", "expiry": "12/28", "cvv": "123",
     "amount": 500000, "currency": "RON", "label": "Sumă foarte mare RON"},
    {"pan": "5500000000000004", "expiry": "12/28", "cvv": "123",
     "amount": 250000, "currency": "RON", "label": "MC sumă mare"},

    # Valută străină → currency_risk=0.4 → score mai ridicat
    {"pan": "4111111111111111", "expiry": "12/28", "cvv": "123",
     "amount": 99900, "currency": "EUR", "label": "VISA EUR"},
    {"pan": "5500000000000004", "expiry": "12/28", "cvv": "123",
     "amount": 149900, "currency": "USD", "label": "MC USD"},
    {"pan": "4111111111111111", "expiry": "12/28", "cvv": "123",
     "amount": 75000, "currency": "EUR", "label": "VISA EUR mediu"},

    # Sumă mare + valută străină → scor maxim (probabil BLOCKED)
    {"pan": "4111111111111111", "expiry": "12/28", "cvv": "123",
     "amount": 400000, "currency": "EUR", "label": "Sumă mare EUR → probabil BLOCKED"},
    {"pan": "5500000000000004", "expiry": "12/28", "cvv": "123",
     "amount": 300000, "currency": "USD", "label": "MC sumă mare USD → probabil BLOCKED"},

    # Card blocat direct de fraud engine
    {"pan": "4000000000000002", "expiry": "12/28", "cvv": "123",
     "amount": 9999, "currency": "RON", "label": "Card blocat (fraud engine)"},
    {"pan": "4000000000000002", "expiry": "12/28", "cvv": "123",
     "amount": 25000, "currency": "EUR", "label": "Card blocat + EUR"},
]

MERCHANTS = [
    "merchant-001", "merchant-002", "merchant-emag",
    "merchant-altex", "merchant-carrefour", "merchant-netflix",
    "merchant-crypto-exchange", "merchant-unknown-intl",
]

ORDERS = [
    "order-telefon", "order-laptop", "order-abonament",
    "order-haine", "order-carti", "order-electronice",
    "order-cosmetice", "order-jucarii", "order-mobilier",
]

AMOUNTS_NORMAL = [
    499, 999, 1499, 2999, 4999, 7999, 12999,
    19999, 24999, 35000, 50000, 75000, 99999,
    150, 250, 350, 600, 800,
]


def get_token() -> str:
    resp = requests.post(
        f"{BASE_URL}/auth/oauth2/token",
        data={"grant_type": "client_credentials", "scope": "payments:read payments:write"},
        auth=(CLIENT_ID, CLIENT_SECRET),
        timeout=10,
    )
    resp.raise_for_status()
    return resp.json()["access_token"]


# IPs externe folosite pentru scenarii de fraudă (ip_external=1 → scor ridicat)
FRAUD_IPS = [
    "91.108.4.100", "185.220.101.5", "194.165.16.78",
    "45.142.212.100", "77.83.198.50", "5.188.206.14",
]


def make_payment(headers: dict, pan: str, expiry: str, cvv: str,
                 amount: int, currency: str, merchant: str, order: str,
                 spoof_ip: str | None = None) -> dict | None:
    try:
        req_headers = dict(headers)
        if spoof_ip:
            req_headers["X-Forwarded-For"] = spoof_ip

        resp = requests.post(
            f"{BASE_URL}/api/v1/payments/authorize",
            json={
                "pan":        pan,
                "expiryDate": expiry,
                "cvv":        cvv,
                "amount":     amount,
                "currency":   currency,
                "merchantId": merchant,
                "orderId":    order,
            },
            headers=req_headers,
            timeout=15,
        )
        return resp.json() if resp.status_code in (200, 201) else None
    except Exception as e:
        print(f"  ERROR: {e}")
        return None


def capture_payment(headers: dict, txn_id: str, amount: int, currency: str) -> bool:
    try:
        resp = requests.post(
            f"{BASE_URL}/api/v1/payments/{txn_id}/capture",
            json={"amount": amount, "currency": currency},
            headers=headers,
            timeout=10,
        )
        return resp.status_code in (200, 201)
    except Exception:
        return False


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--count", type=int, default=25, help="Număr plăți normale de generat")
    parser.add_argument("--capture-rate", type=float, default=0.6,
                        help="Proporție plăți autorizate care sunt și capturate (0-1)")
    parser.add_argument("--no-fraud", action="store_true", help="Sări peste scenariile de fraudă")
    args = parser.parse_args()

    print(f"Obțin token OAuth2 de la {BASE_URL}...")
    token = get_token()
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    print("Token obținut.\n")

    ok = fail = captured = 0
    total = args.count + (0 if args.no_fraud else len(FRAUD_SCENARIOS))

    # --- Plăți normale ---
    print(f"── Plăți normale ({args.count}) ──────────────────────────────")
    for i in range(1, args.count + 1):
        card     = random.choice(CARDS_NORMAL)
        amount   = random.choice(AMOUNTS_NORMAL)
        merchant = random.choice(MERCHANTS[:6])
        order    = f"{random.choice(ORDERS)}-{random.randint(100, 999)}"

        result = make_payment(headers, card["pan"], card["expiry"], card["cvv"],
                              amount, "RON", merchant, order)
        if result:
            txn_id = result.get("txnId", "?")
            status = result.get("status", "?")
            score  = result.get("fraudScore", 0.0)
            print(f"[{i:02d}/{total}] {status:12s} | {amount/100:8.2f} RON | score={score:.2f} | {merchant}")

            if status == "AUTHORIZED" and random.random() < args.capture_rate:
                time.sleep(0.3)
                if capture_payment(headers, txn_id, amount, "RON"):
                    print(f"          └─ CAPTURED")
                    captured += 1
            ok += 1
        else:
            print(f"[{i:02d}/{total}] FAILED")
            fail += 1

        time.sleep(0.5)

    # --- Scenarii fraudă ---
    if not args.no_fraud:
        print(f"\n── Scenarii fraudă ({len(FRAUD_SCENARIOS)}) ──────────────────────────────")
        for j, scenario in enumerate(FRAUD_SCENARIOS, start=args.count + 1):
            merchant = random.choice(MERCHANTS)
            order    = f"fraud-test-{random.randint(1000, 9999)}"

            result = make_payment(
                headers,
                scenario["pan"], scenario["expiry"], scenario["cvv"],
                scenario["amount"], scenario["currency"],
                merchant, order,
                spoof_ip=random.choice(FRAUD_IPS),
            )
            if result:
                txn_id = result.get("txnId", "?")
                status = result.get("status", "?")
                score  = result.get("fraudScore", 0.0)
                amt    = scenario["amount"]
                cur    = scenario["currency"]
                label  = scenario["label"]
                print(f"[{j:02d}/{total}] {status:12s} | {amt/100:8.2f} {cur} | score={score:.2f} | {label}")
                ok += 1
            else:
                print(f"[{j:02d}/{total}] FAILED  | {scenario['label']}")
                fail += 1

            time.sleep(0.5)

    print(f"\nGata! {ok} reușite ({captured} capturate), {fail} eșuate.")
    print("Reîncarcă dashboard-ul pentru a vedea datele noi.")


if __name__ == "__main__":
    main()
