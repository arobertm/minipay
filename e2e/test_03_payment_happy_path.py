"""
Flux complet de plată: authorize → capture → refund.
Testul rulează secvențial — fiecare pas depinde de cel anterior.
"""
import pytest
import requests
from conftest import BASE_URL, CARD_APPROVED, CARD_MASTERCARD


class TestPaymentHappyPath:
    """
    Visa aprobat: authorize → status → capture → status → refund → status final.
    """

    @pytest.fixture(scope="class")
    def authorize_response(self, auth_headers):
        resp = requests.post(
            f"{BASE_URL}/api/v1/payments/authorize",
            json={
                "pan":         CARD_APPROVED["pan"],
                "expiryDate":  CARD_APPROVED["expiry"],
                "cvv":         CARD_APPROVED["cvv"],
                "amount":      10000,
                "currency":    "RON",
                "merchantId":  "merchant-001",
                "orderId":     "e2e-order-visa-happy",
                "description": "E2E test payment",
            },
            headers=auth_headers,
            timeout=15,
        )
        assert resp.status_code in (200, 201), f"Authorize failed: {resp.status_code} {resp.text}"
        return resp.json()

    def test_authorize_returns_authorized(self, authorize_response):
        assert authorize_response["status"] == "AUTHORIZED"

    def test_authorize_has_txn_id(self, authorize_response):
        assert authorize_response.get("txnId"), "txnId must be present"

    def test_authorize_has_dpan_not_pan(self, authorize_response):
        dpan = authorize_response.get("dpan", "")
        # DPAN trebuie să existe și să nu fie același cu PAN-ul original
        assert dpan, "dpan must be present"
        assert dpan != CARD_APPROVED["pan"], "Gateway must never return original PAN"

    def test_authorize_iso_code_approved(self, authorize_response):
        assert authorize_response["isoResponseCode"] == "00"

    def test_authorize_has_auth_code(self, authorize_response):
        assert authorize_response.get("authCode"), "authCode must be present on approval"

    def test_authorize_fraud_score_low(self, authorize_response):
        score = authorize_response.get("fraudScore", 1.0)
        assert score < 0.8, f"Expected low fraud score, got {score}"

    def test_get_payment_status_authorized(self, auth_headers, authorize_response):
        txn_id = authorize_response["txnId"]
        resp = requests.get(
            f"{BASE_URL}/api/v1/payments/{txn_id}",
            headers=auth_headers,
            timeout=10,
        )
        assert resp.status_code == 200
        assert resp.json()["status"] == "AUTHORIZED"

    @pytest.fixture(scope="class")
    def capture_response(self, auth_headers, authorize_response):
        txn_id = authorize_response["txnId"]
        resp = requests.post(
            f"{BASE_URL}/api/v1/payments/{txn_id}/capture",
            json={"amount": 10000, "currency": "RON"},
            headers=auth_headers,
            timeout=10,
        )
        assert resp.status_code == 200, f"Capture failed: {resp.status_code} {resp.text}"
        return resp.json()

    def test_capture_returns_captured(self, capture_response):
        assert capture_response["status"] == "CAPTURED"

    def test_capture_same_txn_id(self, authorize_response, capture_response):
        assert capture_response["txnId"] == authorize_response["txnId"]

    def test_get_payment_status_captured(self, auth_headers, capture_response):
        txn_id = capture_response["txnId"]
        resp = requests.get(
            f"{BASE_URL}/api/v1/payments/{txn_id}",
            headers=auth_headers,
            timeout=10,
        )
        assert resp.status_code == 200
        assert resp.json()["status"] == "CAPTURED"

    @pytest.fixture(scope="class")
    def refund_response(self, auth_headers, capture_response):
        txn_id = capture_response["txnId"]
        resp = requests.post(
            f"{BASE_URL}/api/v1/payments/{txn_id}/refund",
            json={"amount": 10000, "reason": "E2E test refund"},
            headers=auth_headers,
            timeout=10,
        )
        assert resp.status_code == 200, f"Refund failed: {resp.status_code} {resp.text}"
        return resp.json()

    def test_refund_returns_refunded(self, refund_response):
        assert refund_response["status"] == "REFUNDED"

    def test_get_payment_status_refunded(self, auth_headers, refund_response):
        txn_id = refund_response["txnId"]
        resp = requests.get(
            f"{BASE_URL}/api/v1/payments/{txn_id}",
            headers=auth_headers,
            timeout=10,
        )
        assert resp.status_code == 200
        assert resp.json()["status"] == "REFUNDED"


class TestMastercardHappyPath:
    """Mastercard aprobat: authorize → capture."""

    @pytest.fixture(scope="class")
    def authorize_response(self, auth_headers):
        resp = requests.post(
            f"{BASE_URL}/api/v1/payments/authorize",
            json={
                "pan":        CARD_MASTERCARD["pan"],
                "expiryDate": CARD_MASTERCARD["expiry"],
                "cvv":        CARD_MASTERCARD["cvv"],
                "amount":     5000,
                "currency":   "EUR",
                "merchantId": "merchant-002",
                "orderId":    "e2e-order-mc-happy",
                "description": "E2E Mastercard test",
            },
            headers=auth_headers,
            timeout=15,
        )
        assert resp.status_code in (200, 201)
        return resp.json()

    def test_mastercard_authorized(self, authorize_response):
        assert authorize_response["status"] == "AUTHORIZED"

    def test_mastercard_capture(self, auth_headers, authorize_response):
        txn_id = authorize_response["txnId"]
        resp = requests.post(
            f"{BASE_URL}/api/v1/payments/{txn_id}/capture",
            json={"amount": 5000, "currency": "EUR"},
            headers=auth_headers,
            timeout=10,
        )
        assert resp.status_code == 200
        assert resp.json()["status"] == "CAPTURED"
