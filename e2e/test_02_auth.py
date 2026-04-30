"""
Teste autentificare — OAuth2 client_credentials + PQC JWT (Dilithium3).
"""
import pytest
import requests
from conftest import BASE_URL, CLIENT_ID, CLIENT_SECRET


class TestOAuth2:
    def test_token_issued(self, token):
        assert token and len(token) > 20

    def test_token_type_bearer(self):
        resp = requests.post(
            f"{BASE_URL}/auth/oauth2/token",
            data={"grant_type": "client_credentials", "scope": "payments:read payments:write"},
            auth=(CLIENT_ID, CLIENT_SECRET),
            timeout=10,
        )
        assert resp.status_code == 200
        body = resp.json()
        assert body["token_type"].lower() == "bearer"
        assert body["expires_in"] > 0

    def test_invalid_credentials_rejected(self):
        resp = requests.post(
            f"{BASE_URL}/auth/oauth2/token",
            data={"grant_type": "client_credentials"},
            auth=("bad-client", "bad-secret"),
            timeout=10,
        )
        assert resp.status_code == 401

    def test_jwks_endpoint(self):
        resp = requests.get(f"{BASE_URL}/auth/oauth2/jwks", timeout=10)
        assert resp.status_code == 200
        keys = resp.json().get("keys", [])
        assert len(keys) >= 1, "JWKS must contain at least one key"

    def test_pqc_token_issued(self):
        resp = requests.post(
            f"{BASE_URL}/auth/auth/token/pqc",
            params={"subject": "e2e-test", "audience": "minipay", "scope": "payments:read"},
            timeout=10,
        )
        assert resp.status_code == 200
        body = resp.json()
        assert body.get("algorithm") == "DILITHIUM3"
        assert "access_token" in body

    def test_pqc_token_verify(self):
        # obține token PQC
        issue = requests.post(
            f"{BASE_URL}/auth/auth/token/pqc",
            params={"subject": "e2e-test", "audience": "minipay", "scope": "payments:read"},
            timeout=10,
        )
        assert issue.status_code == 200
        pqc_token = issue.json()["access_token"]

        # verifică token
        verify = requests.post(
            f"{BASE_URL}/auth/auth/token/pqc/verify",
            params={"token": pqc_token},
            timeout=10,
        )
        assert verify.status_code == 200
        assert verify.json().get("valid") is True


class TestDilithium3PQC:
    """
    Teste detaliate pentru implementarea CRYSTALS-Dilithium3 (NIST FIPS 204).

    Acoperă:
    - Structura JWT (header, payload, semnătură)
    - Proprietăți criptografice (dimensiune semnătură, algoritm)
    - Verificare semnătură validă/invalidă
    - Respingerea tokenurilor manipulate
    """

    @pytest.fixture(scope="class")
    def pqc_token(self):
        resp = requests.post(
            f"{BASE_URL}/auth/auth/token/pqc",
            params={"subject": "e2e-dilithium", "audience": "minipay",
                    "scope": "payments:read payments:write", "ttl": 3600},
            timeout=10,
        )
        assert resp.status_code == 200
        return resp.json()

    def _decode_part(self, b64url: str) -> dict:
        import json, base64
        padding = 4 - len(b64url) % 4
        if padding != 4:
            b64url += "=" * padding
        return json.loads(base64.urlsafe_b64decode(b64url))

    # --- structura tokenului ---

    def test_token_has_three_parts(self, pqc_token):
        token = pqc_token["access_token"]
        parts = token.split(".")
        assert len(parts) == 3, "JWT compact format trebuie să aibă exact 3 părți: header.payload.signature"

    def test_header_algorithm_is_dilithium3(self, pqc_token):
        header = self._decode_part(pqc_token["access_token"].split(".")[0])
        assert header["alg"] == "DILITHIUM3", \
            f"Headerul JWT trebuie să conțină alg=DILITHIUM3, got {header.get('alg')}"

    def test_header_type_is_jwt(self, pqc_token):
        header = self._decode_part(pqc_token["access_token"].split(".")[0])
        assert header["typ"] == "JWT"

    def test_header_kid_is_dil3(self, pqc_token):
        header = self._decode_part(pqc_token["access_token"].split(".")[0])
        assert header["kid"] == "dil3-1", \
            f"kid trebuie să fie 'dil3-1' (cheia Dilithium3 activă), got {header.get('kid')}"

    # --- claims payload ---

    def test_payload_subject(self, pqc_token):
        payload = self._decode_part(pqc_token["access_token"].split(".")[1])
        assert payload["sub"] == "e2e-dilithium"

    def test_payload_audience(self, pqc_token):
        payload = self._decode_part(pqc_token["access_token"].split(".")[1])
        assert payload["aud"] == "minipay"

    def test_payload_pqc_marker(self, pqc_token):
        payload = self._decode_part(pqc_token["access_token"].split(".")[1])
        assert payload.get("pqc") is True, "Claim 'pqc=true' trebuie prezent pentru a marca tokenul post-quantum"

    def test_payload_alg_family_crystals(self, pqc_token):
        payload = self._decode_part(pqc_token["access_token"].split(".")[1])
        assert payload.get("alg_family") == "CRYSTALS", \
            "Familia algoritmului trebuie să fie CRYSTALS (CRYSTALS-Dilithium)"

    def test_payload_nist_level_3(self, pqc_token):
        payload = self._decode_part(pqc_token["access_token"].split(".")[1])
        assert payload.get("nist_level") == 3, \
            "Dilithium3 corespunde NIST Security Level 3 (echivalent AES-192)"

    def test_payload_expiry_in_future(self, pqc_token):
        import time
        payload = self._decode_part(pqc_token["access_token"].split(".")[1])
        assert payload["exp"] > time.time(), "Tokenul nu trebuie să fie deja expirat"

    def test_payload_issued_at_recent(self, pqc_token):
        import time
        payload = self._decode_part(pqc_token["access_token"].split(".")[1])
        assert abs(payload["iat"] - time.time()) < 60, "iat trebuie să fie în ultimul minut"

    def test_payload_scope(self, pqc_token):
        payload = self._decode_part(pqc_token["access_token"].split(".")[1])
        scope = payload.get("scope", "")
        assert "payments:read" in scope

    # --- proprietăți criptografice ---

    def test_signature_size_dilithium3(self, pqc_token):
        import base64
        sig_b64 = pqc_token["access_token"].split(".")[2]
        padding = 4 - len(sig_b64) % 4
        if padding != 4:
            sig_b64 += "=" * padding
        sig_bytes = base64.urlsafe_b64decode(sig_b64)
        # Dilithium3 (ML-DSA-65) produce semnături de 3309 bytes conform NIST FIPS 204 final.
        # Nota: runda 3 a competiției specifica 3293 bytes — standardul final a modificat structura.
        assert len(sig_bytes) == 3309, \
            f"Semnătura Dilithium3 (ML-DSA-65) trebuie să aibă 3309 bytes, got {len(sig_bytes)}"

    def test_signature_larger_than_rsa(self, pqc_token):
        import base64
        sig_b64 = pqc_token["access_token"].split(".")[2]
        padding = 4 - len(sig_b64) % 4
        if padding != 4:
            sig_b64 += "=" * padding
        sig_bytes = base64.urlsafe_b64decode(sig_b64)
        # RSA-2048 produce semnături de 256 bytes — Dilithium3 este mult mai mare
        assert len(sig_bytes) > 256, \
            "Semnătura post-quantum (Dilithium3) trebuie să fie mai mare decât RSA-2048 (256 bytes)"

    def test_response_metadata(self, pqc_token):
        assert pqc_token.get("algorithm") == "DILITHIUM3"
        assert pqc_token.get("nist_level") == 3

    # --- verificare semnătură ---

    def test_valid_token_passes_verification(self, pqc_token):
        token = pqc_token["access_token"]
        resp = requests.post(
            f"{BASE_URL}/auth/auth/token/pqc/verify",
            params={"token": token},
            timeout=10,
        )
        assert resp.status_code == 200
        assert resp.json().get("valid") is True

    def test_tampered_payload_rejected(self, pqc_token):
        """Modificarea payload-ului trebuie să invalideze semnătura Dilithium3."""
        import base64, json
        parts = pqc_token["access_token"].split(".")

        # decodează și modifică payload-ul (schimbă subject)
        padding = 4 - len(parts[1]) % 4
        padded = parts[1] + ("=" * padding if padding != 4 else "")
        payload = json.loads(base64.urlsafe_b64decode(padded))
        payload["sub"] = "attacker"  # manipulare
        tampered_payload = base64.urlsafe_b64encode(
            json.dumps(payload).encode()
        ).rstrip(b"=").decode()

        tampered_token = f"{parts[0]}.{tampered_payload}.{parts[2]}"

        resp = requests.post(
            f"{BASE_URL}/auth/auth/token/pqc/verify",
            params={"token": tampered_token},
            timeout=10,
        )
        assert resp.status_code == 200
        assert resp.json().get("valid") is False, \
            "Un token cu payload manipulat trebuie să fie respins — semnătura Dilithium3 nu mai corespunde"

    def test_tampered_signature_rejected(self, pqc_token):
        """Modificarea semnăturii (ultimul byte) trebuie să fie respinsă."""
        import base64
        parts = pqc_token["access_token"].split(".")
        padding = 4 - len(parts[2]) % 4
        padded = parts[2] + ("=" * padding if padding != 4 else "")
        sig_bytes = bytearray(base64.urlsafe_b64decode(padded))
        sig_bytes[-1] ^= 0xFF  # flip ultimul byte
        corrupted_sig = base64.urlsafe_b64encode(bytes(sig_bytes)).rstrip(b"=").decode()

        corrupted_token = f"{parts[0]}.{parts[1]}.{corrupted_sig}"

        resp = requests.post(
            f"{BASE_URL}/auth/auth/token/pqc/verify",
            params={"token": corrupted_token},
            timeout=10,
        )
        assert resp.status_code == 200
        assert resp.json().get("valid") is False, \
            "Un token cu semnătură coruptă trebuie respins"

    def test_malformed_token_rejected(self):
        """Un string aleatoriu nu trebuie să fie acceptat ca token valid."""
        resp = requests.post(
            f"{BASE_URL}/auth/auth/token/pqc/verify",
            params={"token": "not.a.validtoken"},
            timeout=10,
        )
        assert resp.status_code == 200
        assert resp.json().get("valid") is False

    def test_empty_token_rejected(self):
        resp = requests.post(
            f"{BASE_URL}/auth/auth/token/pqc/verify",
            params={"token": ""},
            timeout=10,
        )
        assert resp.status_code in (200, 400)
        if resp.status_code == 200:
            assert resp.json().get("valid") is False
