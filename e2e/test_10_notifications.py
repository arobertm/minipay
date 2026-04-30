"""
Teste notif-svc: notificări generate de Kafka după plăți.
"""
import pytest
import time
import requests
from conftest import BASE_URL, CARD_APPROVED


class TestNotifications:
    def test_notifications_endpoint_accessible(self, auth_headers):
        resp = requests.get(
            f"{BASE_URL}/notif/notifications",
            headers=auth_headers,
            timeout=10,
        )
        assert resp.status_code == 200
        assert isinstance(resp.json(), list)

    def test_notifications_stats_accessible(self, auth_headers):
        resp = requests.get(
            f"{BASE_URL}/notif/notifications/stats",
            headers=auth_headers,
            timeout=10,
        )
        assert resp.status_code == 200

    def test_payment_generates_notification(self, auth_headers):
        """O plată procesată trebuie să genereze o notificare via Kafka."""
        # Plată nouă
        pay_resp = requests.post(
            f"{BASE_URL}/api/v1/payments/authorize",
            json={
                "pan":        CARD_APPROVED["pan"],
                "expiryDate": CARD_APPROVED["expiry"],
                "cvv":        CARD_APPROVED["cvv"],
                "amount":     2500,
                "currency":   "RON",
                "merchantId": "merchant-001",
                "orderId":    "e2e-notif-trace",
            },
            headers=auth_headers,
            timeout=15,
        )
        assert pay_resp.status_code in (200, 201)
        txn_id = pay_resp.json()["txnId"]

        # Kafka consumer are nevoie de câteva secunde să proceseze
        time.sleep(3)

        resp = requests.get(
            f"{BASE_URL}/notif/notifications/{txn_id}",
            headers=auth_headers,
            timeout=10,
        )
        assert resp.status_code == 200, \
            f"No notification found for txnId {txn_id}"
        notifs = resp.json()
        assert len(notifs) >= 1, "At least one notification expected"
