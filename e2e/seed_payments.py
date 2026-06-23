"""
Seed script — generează plăți de test pentru a popula dashboard-ul.
Rulare: python seed_payments.py [--count 30]
"""
import argparse
import random
import time
import requests

BASE_URL    = "https://api-minipay.online"
CLIENT_ID   = "demo-client"
CLIENT_SECRET = "demo-secret"

CARDS = [
    {"pan": "4111111111111111", "expiry": "12/28", "cvv": "123", "label": "VISA approved"},
    {"pan": "5500000000000004", "expiry": "12/28", "cvv": "123", "label": "MC approved"},
    {"pan": "4000000000009995", "expiry": "12/28", "cvv": "456", "label": "VISA insuf funds"},
]

MERCHANTS = [
    "merchant-001", "merchant-002", "merchant-emag",
    "merchant-altex", "merchant-carrefour", "merchant-netflix",
]

ORDERS = [
    "order-telefon", "order-laptop", "order-abonament",
    "order-haine", "order-carti", "order-electronice",
    "order-cosmetice", "order-jucarii", "order-mobilier",
]

AMOUNTS = [
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


def make_payment(headers: dict, card: dict, amount: int, merchant: str, order: str) -> dict | None:
    try:
        resp = requests.post(
            f"{BASE_URL}/api/v1/payments/authorize",
            json={
                "pan":        card["pan"],
                "expiryDate": card["expiry"],
                "cvv":        card["cvv"],
                "amount":     amount,
                "currency":   "RON",
                "merchantId": merchant,
                "orderId":    order,
            },
            headers=headers,
            timeout=15,
        )
        return resp.json() if resp.status_code in (200, 201) else None
    except Exception as e:
        print(f"  ERROR: {e}")
        return None


def capture_payment(headers: dict, txn_id: str, amount: int) -> bool:
    try:
        resp = requests.post(
            f"{BASE_URL}/api/v1/payments/{txn_id}/capture",
            json={"amount": amount, "currency": "RON"},
            headers=headers,
            timeout=10,
        )
        return resp.status_code in (200, 201)
    except Exception:
        return False


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--count", type=int, default=25, help="Număr plăți de generat")
    parser.add_argument("--capture-rate", type=float, default=0.6,
                        help="Proporție plăți autorizate care sunt și capturate (0-1)")
    args = parser.parse_args()

    print(f"Obțin token OAuth2 de la {BASE_URL}...")
    token = get_token()
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    print("Token obținut.\n")

    ok = fail = captured = 0

    for i in range(1, args.count + 1):
        card     = random.choice(CARDS)
        amount   = random.choice(AMOUNTS)
        merchant = random.choice(MERCHANTS)
        order    = f"{random.choice(ORDERS)}-{random.randint(100, 999)}"

        result = make_payment(headers, card, amount, merchant, order)

        if result:
            txn_id = result.get("txnId", "?")
            status = result.get("status", "?")
            print(f"[{i:02d}/{args.count}] {status:12s} | {amount/100:8.2f} RON | {merchant} | txnId={txn_id[:8]}...")

            if status == "AUTHORIZED" and random.random() < args.capture_rate:
                time.sleep(0.3)
                if capture_payment(headers, txn_id, amount):
                    print(f"         └─ CAPTURED")
                    captured += 1
            ok += 1
        else:
            print(f"[{i:02d}/{args.count}] FAILED  | {amount/100:8.2f} RON | {merchant}")
            fail += 1

        time.sleep(0.5)

    print(f"\nGata! {ok} reușite ({captured} capturate), {fail} eșuate.")
    print("Reîncarcă dashboard-ul pentru a vedea datele noi.")


if __name__ == "__main__":
    main()
