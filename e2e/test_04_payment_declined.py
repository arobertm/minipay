"""
Scenarii negative de plată: card blocat, fonduri insuficiente, validare input.
"""
import pytest
import requests
from conftest import BASE_URL, CARD_BLOCKED, CARD_INSUF_FUNDS, CARD_APPROVED


class TestDeclinedScenarios:
    def test_blocked_card_declined(self, auth_headers):
        resp = requests.post(
            f"{BASE_URL}/api/v1/payments/authorize",
            json={
                "pan":        CARD_BLOCKED["pan"],
                "expiryDate": CARD_BLOCKED["expiry"],
                "cvv":        CARD_BLOCKED["cvv"],
                "amount":     10000,
                "currency":   "RON",
                "merchantId": "merchant-001",
                "orderId":    "e2e-order-blocked",
            },
            headers=auth_headers,
            timeout=15,
        )
        assert resp.status_code in (200, 201)
        body = resp.json()
        assert body["status"] in ("DECLINED", "BLOCKED"), \
            f"Expected DECLINED/BLOCKED for blocked card, got {body['status']}"
        assert body["isoResponseCode"] == "05"

    def test_insufficient_funds_declined(self, auth_headers):
        resp = requests.post(
            f"{BASE_URL}/api/v1/payments/authorize",
            json={
                "pan":        CARD_INSUF_FUNDS["pan"],
                "expiryDate": CARD_INSUF_FUNDS["expiry"],
                "cvv":        CARD_INSUF_FUNDS["cvv"],
                "amount":     10000,
                "currency":   "RON",
                "merchantId": "merchant-001",
                "orderId":    "e2e-order-insuf",
            },
            headers=auth_headers,
            timeout=15,
        )
        assert resp.status_code in (200, 201)
        body = resp.json()
        assert body["status"] == "DECLINED"
        assert body["isoResponseCode"] == "51"

    def test_capture_on_declined_returns_409(self, auth_headers):
        # Autorizează cu card blocat — va fi DECLINED
        auth_resp = requests.post(
            f"{BASE_URL}/api/v1/payments/authorize",
            json={
                "pan":        CARD_BLOCKED["pan"],
                "expiryDate": CARD_BLOCKED["expiry"],
                "cvv":        CARD_BLOCKED["cvv"],
                "amount":     500,
                "currency":   "RON",
                "merchantId": "merchant-001",
                "orderId":    "e2e-order-capture-declined",
            },
            headers=auth_headers,
            timeout=15,
        )
        txn_id = auth_resp.json()["txnId"]

        # Încearcă capture pe o tranzacție DECLINED — trebuie 409
        cap_resp = requests.post(
            f"{BASE_URL}/api/v1/payments/{txn_id}/capture",
            json={"amount": 500, "currency": "RON"},
            headers=auth_headers,
            timeout=10,
        )
        assert cap_resp.status_code == 409, \
            f"Capture on DECLINED should return 409, got {cap_resp.status_code}"

    def test_refund_on_authorized_returns_409(self, auth_headers):
        # Autorizează fără capture
        auth_resp = requests.post(
            f"{BASE_URL}/api/v1/payments/authorize",
            json={
                "pan":        CARD_APPROVED["pan"],
                "expiryDate": CARD_APPROVED["expiry"],
                "cvv":        CARD_APPROVED["cvv"],
                "amount":     1000,
                "currency":   "RON",
                "merchantId": "merchant-001",
                "orderId":    "e2e-order-refund-noCapture",
            },
            headers=auth_headers,
            timeout=15,
        )
        assert auth_resp.json()["status"] == "AUTHORIZED"
        txn_id = auth_resp.json()["txnId"]

        # Încearcă refund direct pe AUTHORIZED (fără capture prealabil) — trebuie 409
        ref_resp = requests.post(
            f"{BASE_URL}/api/v1/payments/{txn_id}/refund",
            json={"amount": 1000, "reason": "test refund without capture"},
            headers=auth_headers,
            timeout=10,
        )
        assert ref_resp.status_code == 409, \
            f"Refund on AUTHORIZED (not CAPTURED) should return 409, got {ref_resp.status_code}"

    def test_get_nonexistent_payment_returns_404(self, auth_headers):
        resp = requests.get(
            f"{BASE_URL}/api/v1/payments/00000000-0000-0000-0000-000000000000",
            headers=auth_headers,
            timeout=10,
        )
        assert resp.status_code == 404

    def test_unauthorized_request_returns_401(self):
        resp = requests.post(
            f"{BASE_URL}/api/v1/payments/authorize",
            json={
                "pan": "4111111111111111", "expiryDate": "12/28",
                "cvv": "123", "amount": 100, "currency": "RON",
                "merchantId": "m1", "orderId": "o1",
            },
            headers={"Content-Type": "application/json"},
            timeout=10,
        )
        assert resp.status_code == 401


class TestInputValidation:
    def test_invalid_pan_format(self, auth_headers):
        resp = requests.post(
            f"{BASE_URL}/api/v1/payments/authorize",
            json={
                "pan": "not-a-pan", "expiryDate": "12/28",
                "cvv": "123", "amount": 1000, "currency": "RON",
                "merchantId": "merchant-001", "orderId": "e2e-invalid-pan",
            },
            headers=auth_headers,
            timeout=10,
        )
        assert resp.status_code == 400

    def test_invalid_expiry_format(self, auth_headers):
        resp = requests.post(
            f"{BASE_URL}/api/v1/payments/authorize",
            json={
                "pan": "4111111111111111", "expiryDate": "1228",
                "cvv": "123", "amount": 1000, "currency": "RON",
                "merchantId": "merchant-001", "orderId": "e2e-invalid-expiry",
            },
            headers=auth_headers,
            timeout=10,
        )
        assert resp.status_code == 400, "expiryDate '1228' must fail (should be MM/YY)"

    def test_zero_amount_rejected(self, auth_headers):
        resp = requests.post(
            f"{BASE_URL}/api/v1/payments/authorize",
            json={
                "pan": "4111111111111111", "expiryDate": "12/28",
                "cvv": "123", "amount": 0, "currency": "RON",
                "merchantId": "merchant-001", "orderId": "e2e-zero-amount",
            },
            headers=auth_headers,
            timeout=10,
        )
        assert resp.status_code == 400

    def test_wrong_currency_length_rejected(self, auth_headers):
        resp = requests.post(
            f"{BASE_URL}/api/v1/payments/authorize",
            json={
                "pan": "4111111111111111", "expiryDate": "12/28",
                "cvv": "123", "amount": 1000, "currency": "RO",
                "merchantId": "merchant-001", "orderId": "e2e-bad-currency",
            },
            headers=auth_headers,
            timeout=10,
        )
        assert resp.status_code == 400
