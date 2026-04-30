"""
Teste tds-svc: 3DS2 — frictionless (low fraud) + OTP challenge (high fraud).
"""
import pytest
import requests
from conftest import BASE_URL, CARD_APPROVED


class TestThreeDSecure:
    def test_frictionless_low_fraud_score(self, auth_headers):
        """Tranzacție cu fraudScore < 0.5 → autentificare frictionless (Y/A)."""
        resp = requests.post(
            f"{BASE_URL}/tds/authenticate",
            json={
                "pan":        CARD_APPROVED["pan"],
                "expiryDate": CARD_APPROVED["expiry"],
                "amount":     1000,
                "currency":   "RON",
                "merchantId": "merchant-001",
                "fraudScore": 0.2,
            },
            headers=auth_headers,
            timeout=10,
        )
        assert resp.status_code in (200, 201)
        body = resp.json()
        assert body.get("transStatus") in ("Y", "A"), \
            f"Expected frictionless (Y/A), got {body.get('transStatus')}"

    def test_challenge_high_fraud_score(self, auth_headers):
        """Tranzacție cu fraudScore >= 0.7 → OTP challenge (C)."""
        resp = requests.post(
            f"{BASE_URL}/tds/authenticate",
            json={
                "pan":        CARD_APPROVED["pan"],
                "expiryDate": CARD_APPROVED["expiry"],
                "amount":     50000,
                "currency":   "RON",
                "merchantId": "merchant-risky",
                "fraudScore": 0.75,
            },
            headers=auth_headers,
            timeout=10,
        )
        assert resp.status_code in (200, 201)
        body = resp.json()
        assert body.get("transStatus") == "C", \
            f"Expected OTP challenge (C), got {body.get('transStatus')}"
        # câmpul din AuthenticationResult record se numește acsTransID (D mare)
        assert body.get("acsTransID"), "acsTransID must be present for challenge"

    def test_otp_challenge_flow(self, auth_headers):
        """Flux complet OTP: authenticate → get challenge → submit OTP."""
        # 1. Inițiază challenge
        auth_resp = requests.post(
            f"{BASE_URL}/tds/authenticate",
            json={
                "pan": CARD_APPROVED["pan"], "expiryDate": CARD_APPROVED["expiry"],
                "amount": 50000, "currency": "RON",
                "merchantId": "merchant-risky", "fraudScore": 0.75,
            },
            headers=auth_headers,
            timeout=10,
        )
        assert auth_resp.status_code in (200, 201)
        acs_trans_id = auth_resp.json().get("acsTransID")
        if not acs_trans_id:
            pytest.skip("No challenge initiated (frictionless path)")

        # 2. Obține OTP-ul (demo mode)
        challenge_resp = requests.get(
            f"{BASE_URL}/tds/challenge/{acs_trans_id}",
            headers=auth_headers,
            timeout=10,
        )
        assert challenge_resp.status_code == 200
        otp = challenge_resp.json().get("otp_demo_only")
        assert otp, "OTP must be present in challenge response (demo mode)"

        # 3. Trimite OTP-ul corect
        submit_resp = requests.post(
            f"{BASE_URL}/tds/challenge/{acs_trans_id}",
            json={"otp": otp},
            headers=auth_headers,
            timeout=10,
        )
        assert submit_resp.status_code == 200
        body = submit_resp.json()
        assert body.get("transStatus") in ("Y", "A"), \
            f"After correct OTP, expected Y/A, got {body.get('transStatus')}"

    def test_wrong_otp_rejected(self, auth_headers):
        """OTP greșit trebuie să returneze autentificare eșuată (N/U)."""
        auth_resp = requests.post(
            f"{BASE_URL}/tds/authenticate",
            json={
                "pan": CARD_APPROVED["pan"], "expiryDate": CARD_APPROVED["expiry"],
                "amount": 50000, "currency": "RON",
                "merchantId": "merchant-risky", "fraudScore": 0.75,
            },
            headers=auth_headers,
            timeout=10,
        )
        acs_trans_id = auth_resp.json().get("acsTransID")
        if not acs_trans_id:
            pytest.skip("No challenge initiated")

        submit_resp = requests.post(
            f"{BASE_URL}/tds/challenge/{acs_trans_id}",
            json={"otp": "000000"},
            headers=auth_headers,
            timeout=10,
        )
        assert submit_resp.status_code in (200, 400)
        if submit_resp.status_code == 200:
            assert submit_resp.json().get("transStatus") in ("N", "U"), \
                "Wrong OTP must result in failed authentication (N/U)"
