"""
Teste vault-svc: tokenizare PAN → DPAN (AES-256-GCM) + GDPR delete.
"""
import pytest
import requests
from conftest import BASE_URL, CARD_APPROVED


class TestVault:
    @pytest.fixture(scope="class")
    def tokenize_response(self, auth_headers):
        resp = requests.post(
            f"{BASE_URL}/vault/tokenize",
            json={
                "pan":       CARD_APPROVED["pan"],
                "expiryDate": CARD_APPROVED["expiry"],
                "requestId": "e2e-vault-test-001",
            },
            headers=auth_headers,
            timeout=10,
        )
        assert resp.status_code == 201, f"Tokenize failed: {resp.status_code} {resp.text}"
        return resp.json()

    def test_tokenize_returns_dpan(self, tokenize_response):
        assert "dpan" in tokenize_response
        assert tokenize_response["dpan"] != CARD_APPROVED["pan"], \
            "DPAN must differ from original PAN"

    def test_dpan_preserves_bin(self, tokenize_response):
        original_bin = CARD_APPROVED["pan"][:6]
        dpan_bin = tokenize_response["dpan"][:6]
        assert dpan_bin == original_bin, \
            f"BIN must be preserved: expected {original_bin}, got {dpan_bin}"

    def test_dpan_passes_luhn(self, tokenize_response):
        dpan = tokenize_response["dpan"]

        def luhn_check(number):
            digits = [int(d) for d in str(number)]
            odd_digits = digits[-1::-2]
            even_digits = digits[-2::-2]
            total = sum(odd_digits)
            for d in even_digits:
                total += sum(divmod(d * 2, 10))
            return total % 10 == 0

        assert luhn_check(dpan), f"DPAN {dpan} failed Luhn check"

    def test_detokenize_recovers_original_pan(self, auth_headers, tokenize_response):
        dpan = tokenize_response["dpan"]
        resp = requests.post(
            f"{BASE_URL}/vault/detokenize/{dpan}",
            headers=auth_headers,
            timeout=10,
        )
        assert resp.status_code == 200
        body = resp.json()
        assert body["pan"] == CARD_APPROVED["pan"], \
            "Detokenize must return original PAN"

    def test_multiple_tokenize_calls_each_return_valid_dpan(self, auth_headers):
        """Fiecare apel tokenize returnează un DPAN unic și valid (design single-use)."""
        r1 = requests.post(
            f"{BASE_URL}/vault/tokenize",
            json={"pan": CARD_APPROVED["pan"], "expiryDate": CARD_APPROVED["expiry"], "requestId": "req-A"},
            headers=auth_headers,
            timeout=10,
        )
        r2 = requests.post(
            f"{BASE_URL}/vault/tokenize",
            json={"pan": CARD_APPROVED["pan"], "expiryDate": CARD_APPROVED["expiry"], "requestId": "req-B"},
            headers=auth_headers,
            timeout=10,
        )
        assert r1.status_code == 201 and r2.status_code == 201
        d1, d2 = r1.json()["dpan"], r2.json()["dpan"]
        # Vault generează DPAN-uri unice per tokenizare (single-use capable)
        assert d1 != d2, "Each tokenize call should return a unique DPAN"
        # Ambele trebuie să aibă același BIN
        assert d1[:6] == CARD_APPROVED["pan"][:6]
        assert d2[:6] == CARD_APPROVED["pan"][:6]

    def test_delete_token_gdpr(self, auth_headers):
        """GDPR Art.17 — tokenul trebuie să poată fi șters."""
        # Tokenizare card temporar
        tok = requests.post(
            f"{BASE_URL}/vault/tokenize",
            json={"pan": "5500000000000004", "expiryDate": "12/28", "requestId": "e2e-gdpr-delete"},
            headers=auth_headers,
            timeout=10,
        )
        assert tok.status_code == 201
        dpan = tok.json()["dpan"]

        # Ștergere token
        del_resp = requests.delete(
            f"{BASE_URL}/vault/tokens/{dpan}",
            headers=auth_headers,
            timeout=10,
        )
        assert del_resp.status_code == 204

        # Detokenizare după ștergere trebuie să returneze 404
        det_resp = requests.post(
            f"{BASE_URL}/vault/detokenize/{dpan}",
            headers=auth_headers,
            timeout=10,
        )
        assert det_resp.status_code == 404, \
            "After GDPR deletion, detokenize must return 404"
