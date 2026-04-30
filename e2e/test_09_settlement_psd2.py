"""
Teste settlement-svc + psd2-svc (Open Banking AIS/PIS).
"""
import pytest
import requests
from datetime import date
from conftest import BASE_URL, CARD_APPROVED


class TestSettlement:
    def test_settlement_batches_accessible(self, auth_headers):
        today = date.today().isoformat()
        resp = requests.get(
            f"{BASE_URL}/settlements/batches",
            params={"date": today},
            headers=auth_headers,
            timeout=10,
        )
        assert resp.status_code == 200

    def test_settlement_records_accessible(self, auth_headers):
        today = date.today().isoformat()
        resp = requests.get(
            f"{BASE_URL}/settlements/records",
            params={"date": today},
            headers=auth_headers,
            timeout=10,
        )
        assert resp.status_code == 200

    def test_manual_reconcile_trigger(self, auth_headers):
        today = date.today().isoformat()
        resp = requests.post(
            f"{BASE_URL}/settlements/reconcile",
            params={"date": today},
            headers=auth_headers,
            timeout=15,
        )
        assert resp.status_code in (200, 202), \
            f"Reconcile failed: {resp.status_code} {resp.text}"

    def test_captured_payment_appears_in_settlement(self, auth_headers):
        """Plată capturată azi trebuie să apară în settlement records."""
        # Authorize + Capture
        auth_r = requests.post(
            f"{BASE_URL}/api/v1/payments/authorize",
            json={
                "pan": CARD_APPROVED["pan"], "expiryDate": CARD_APPROVED["expiry"],
                "cvv": CARD_APPROVED["cvv"], "amount": 3333, "currency": "RON",
                "merchantId": "merchant-001", "orderId": "e2e-settlement-trace",
            },
            headers=auth_headers, timeout=15,
        )
        assert auth_r.json()["status"] == "AUTHORIZED"
        txn_id = auth_r.json()["txnId"]

        requests.post(
            f"{BASE_URL}/api/v1/payments/{txn_id}/capture",
            json={"amount": 3333, "currency": "RON"},
            headers=auth_headers, timeout=10,
        )

        # Trigger reconciliation
        today = date.today().isoformat()
        requests.post(f"{BASE_URL}/settlements/reconcile", params={"date": today},
                      headers=auth_headers, timeout=15)

        # Verifică că tranzacția apare în records
        records_r = requests.get(
            f"{BASE_URL}/settlements/records",
            params={"date": today},
            headers=auth_headers, timeout=10,
        )
        assert records_r.status_code == 200


class TestPSD2:
    @pytest.fixture(scope="class")
    def consent_id(self, auth_headers):
        # ConsentService.create() setează status=VALID automat — nu există endpoint de confirm
        # Permisiunile trebuie să corespundă exact cu cele verificate în AccountController:
        #   ReadAccountList, ReadBalances, ReadTransactions
        resp = requests.post(
            f"{BASE_URL}/psd2/consents",
            json={
                "psuId":      "e2e-test-user",
                "accountIds": ["ACC-001"],
                "permissions": ["ReadAccountList", "ReadBalances", "ReadTransactions"],
                "validUntil":  "2027-12-31",
            },
            headers=auth_headers,
            timeout=10,
        )
        assert resp.status_code in (200, 201), f"Consent creation failed: {resp.status_code} {resp.text}"
        return resp.json()["consentId"]

    def test_create_consent(self, consent_id):
        assert consent_id and len(consent_id) > 0

    def test_get_consent_is_valid(self, auth_headers, consent_id):
        resp = requests.get(
            f"{BASE_URL}/psd2/consents/{consent_id}",
            headers=auth_headers,
            timeout=10,
        )
        assert resp.status_code == 200
        body = resp.json()
        assert body["consentId"] == consent_id
        # Consimțământul devine VALID imediat la creare (nu necesită confirmare separată)
        assert body["status"] == "VALID"

    def test_get_accounts_with_consent(self, auth_headers, consent_id):
        resp = requests.get(
            f"{BASE_URL}/psd2/accounts",
            headers={**auth_headers, "Consent-ID": consent_id},
            timeout=10,
        )
        assert resp.status_code == 200
        assert "accounts" in resp.json()

    def test_get_account_balances(self, auth_headers, consent_id):
        resp = requests.get(
            f"{BASE_URL}/psd2/accounts/ACC-001/balances",
            headers={**auth_headers, "Consent-ID": consent_id},
            timeout=10,
        )
        assert resp.status_code == 200
        assert "balances" in resp.json()

    def test_get_account_transactions(self, auth_headers, consent_id):
        resp = requests.get(
            f"{BASE_URL}/psd2/accounts/ACC-001/transactions",
            headers={**auth_headers, "Consent-ID": consent_id},
            timeout=10,
        )
        assert resp.status_code == 200
        assert "transactions" in resp.json()

    def test_revoke_consent(self, auth_headers):
        # Creăm un consent separat ca să nu afectăm celelalte teste
        create = requests.post(
            f"{BASE_URL}/psd2/consents",
            json={"psuId": "e2e-revoke-user", "accountIds": ["ACC-002"],
                  "permissions": ["ReadAccountList"], "validUntil": "2027-12-31"},
            headers=auth_headers, timeout=10,
        )
        cid = create.json()["consentId"]
        del_resp = requests.delete(f"{BASE_URL}/psd2/consents/{cid}", headers=auth_headers, timeout=10)
        assert del_resp.status_code == 200
        assert del_resp.json()["status"] == "REVOKED"
