"""
Teste fraud-svc: scoring XGBoost + SHAP explainability.
"""
import pytest
import requests
from conftest import BASE_URL


class TestFraudScoring:
    def test_low_risk_transaction_allowed(self):
        resp = requests.post(
            f"{BASE_URL}/fraud/score",
            json={
                "dpan":       "4111110000001234",
                "amount":     5000,
                "currency":   "RON",
                "merchantId": "merchant-001",
                "ipAddress":  "192.168.1.100",
            },
            timeout=10,
        )
        assert resp.status_code == 200
        body = resp.json()
        assert body["decision"] == "ALLOW"
        assert body["score"] < 0.5

    def test_high_risk_transaction_blocked(self):
        resp = requests.post(
            f"{BASE_URL}/fraud/score",
            json={
                "dpan":       "4000000000009999",
                "amount":     999999,
                "currency":   "USD",
                "merchantId": "merchant-risky",
                "ipAddress":  "185.220.101.1",
            },
            timeout=10,
        )
        assert resp.status_code == 200
        body = resp.json()
        assert body["decision"] in ("BLOCK", "CHALLENGE")
        assert body["score"] >= 0.5

    def test_response_contains_shap_details(self):
        resp = requests.post(
            f"{BASE_URL}/fraud/score",
            json={
                "dpan":       "4111110000001234",
                "amount":     5000,
                "currency":   "RON",
                "merchantId": "merchant-001",
            },
            timeout=10,
        )
        assert resp.status_code == 200
        body = resp.json()
        assert "shap_details" in body, "SHAP details required for GDPR Art.22 compliance"
        assert len(body["shap_details"]) > 0

    def test_shap_details_have_required_fields(self):
        resp = requests.post(
            f"{BASE_URL}/fraud/score",
            json={"dpan": "4111110000001234", "amount": 5000, "currency": "RON", "merchantId": "m1"},
            timeout=10,
        )
        body = resp.json()
        for detail in body["shap_details"]:
            assert "feature" in detail
            assert "shap_value" in detail
            assert "description" in detail

    def test_score_range_is_valid(self):
        resp = requests.post(
            f"{BASE_URL}/fraud/score",
            json={"dpan": "4111110000001234", "amount": 5000, "currency": "RON", "merchantId": "m1"},
            timeout=10,
        )
        score = resp.json()["score"]
        assert 0.0 <= score <= 1.0, f"Fraud score {score} out of [0, 1] range"

    def test_block_decision_has_reasons(self):
        resp = requests.post(
            f"{BASE_URL}/fraud/score",
            json={
                "dpan":       "4000000000009999",
                "amount":     999999,
                "currency":   "USD",
                "merchantId": "merchant-risky",
                "ipAddress":  "185.220.101.1",
            },
            timeout=10,
        )
        body = resp.json()
        if body["decision"] == "BLOCK":
            assert len(body.get("reasons", [])) > 0, \
                "BLOCK decision must include reasons (GDPR Art.22)"

    def test_fraud_health(self):
        # FastAPI expune /health intern, dar fără strip prefix nu e accesibil la /fraud/health.
        # Verificăm că serviciul răspunde prin endpoint-ul funcțional /fraud/score.
        resp = requests.post(
            f"{BASE_URL}/fraud/score",
            json={"dpan": "4111110000001234", "amount": 100, "currency": "RON", "merchantId": "health-check"},
            timeout=10,
        )
        assert resp.status_code == 200, f"fraud-svc unreachable: {resp.status_code}"

    def test_high_fraud_score_blocks_payment(self, auth_headers):
        """Plată cu scor fraud ridicat trebuie să returneze BLOCKED în gateway."""
        resp = requests.post(
            f"{BASE_URL}/api/v1/payments/authorize",
            json={
                "pan":        "4111111111111111",
                "expiryDate": "12/28",
                "cvv":        "123",
                "amount":     999999,
                "currency":   "USD",
                "merchantId": "merchant-risky",
                "orderId":    "e2e-fraud-block-test",
                "description": "High value suspicious transaction",
            },
            headers=auth_headers,
            timeout=15,
        )
        assert resp.status_code in (200, 201)
        body = resp.json()
        assert body["status"] in ("BLOCKED", "CHALLENGE", "DECLINED"), \
            f"Expected fraud block, got {body['status']}"
