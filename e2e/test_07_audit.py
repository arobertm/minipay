"""
Teste audit-svc: Merkle Tree hash chain + integritate imutabilă.
"""
import pytest
import requests
from conftest import BASE_URL, CARD_APPROVED


class TestAuditLog:
    def test_audit_entries_accessible(self, auth_headers):
        resp = requests.get(
            f"{BASE_URL}/audit/entries",
            params={"page": 0, "size": 10},
            headers=auth_headers,
            timeout=10,
        )
        assert resp.status_code == 200
        body = resp.json()
        assert "content" in body
        assert "totalElements" in body

    def test_audit_chain_is_valid(self, auth_headers):
        resp = requests.get(
            f"{BASE_URL}/audit/verify",
            headers=auth_headers,
            timeout=10,
        )
        assert resp.status_code == 200
        body = resp.json()
        assert body.get("isValid") is True, \
            f"Merkle chain integrity check failed: {body.get('message')}"

    def test_payment_creates_audit_entry(self, auth_headers):
        """Verifică că o plată nouă apare în audit log."""
        # Fă o plată nouă
        pay_resp = requests.post(
            f"{BASE_URL}/api/v1/payments/authorize",
            json={
                "pan":        CARD_APPROVED["pan"],
                "expiryDate": CARD_APPROVED["expiry"],
                "cvv":        CARD_APPROVED["cvv"],
                "amount":     7777,
                "currency":   "RON",
                "merchantId": "merchant-001",
                "orderId":    "e2e-audit-trace-test",
            },
            headers=auth_headers,
            timeout=15,
        )
        assert pay_resp.status_code in (200, 201)
        txn_id = pay_resp.json()["txnId"]

        # Caută în audit log după txnId
        audit_resp = requests.get(
            f"{BASE_URL}/audit/entries/{txn_id}",
            headers=auth_headers,
            timeout=10,
        )
        assert audit_resp.status_code == 200, \
            f"Payment {txn_id} not found in audit log"

    def test_audit_entry_has_hash(self, auth_headers):
        resp = requests.get(
            f"{BASE_URL}/audit/entries",
            params={"page": 0, "size": 1},
            headers=auth_headers,
            timeout=10,
        )
        entries = resp.json().get("content", [])
        if entries:
            entry = entries[0]
            assert "entryHash" in entry, "Audit entry must have entryHash"
            assert len(entry["entryHash"]) == 64, "entryHash must be SHA-256 (64 hex chars)"

    def test_audit_chain_still_valid_after_payment(self, auth_headers):
        """Integritatea chain-ului Merkle rămâne validă după adăugarea de intrări noi."""
        # Fă o plată
        requests.post(
            f"{BASE_URL}/api/v1/payments/authorize",
            json={
                "pan": CARD_APPROVED["pan"], "expiryDate": CARD_APPROVED["expiry"],
                "cvv": CARD_APPROVED["cvv"], "amount": 1111, "currency": "RON",
                "merchantId": "merchant-001", "orderId": "e2e-chain-integrity",
            },
            headers=auth_headers,
            timeout=15,
        )
        # Verifică că chain-ul este în continuare valid
        resp = requests.get(f"{BASE_URL}/audit/verify", headers=auth_headers, timeout=10)
        assert resp.json().get("isValid") is True
