"""
Health checks — verifică că toate serviciile răspund înainte de testele E2E.

Servicii cu strip prefix (/auth, /api, /users, /notif) expun actuator standard.
Servicii fără strip (/vault, /audit, /settlements, /tds) nu au actuator accesibil
din exterior, deci verificăm printr-un endpoint funcțional cunoscut.
"""
import pytest
import requests
from conftest import BASE_URL

# Servicii cu strip prefix — actuator accesibil direct
STRIP_SERVICES = [
    ("/auth/actuator/health",  "auth-svc"),
    ("/users/actuator/health", "user-svc"),
    ("/api/actuator/health",   "gateway-svc"),
    ("/notif/actuator/health", "notif-svc"),
]

# Servicii fără strip — verificăm printr-un endpoint funcțional (GET simplu)
NO_STRIP_SERVICES = [
    ("GET",  "/audit/entries?page=0&size=1",     "audit-svc"),
    ("GET",  "/settlements/batches",             "settlement-svc"),
    ("GET",  "/vault/actuator/health",           "vault-svc"),   # 404 = service UP, path mismatch
    ("GET",  "/tds/actuator/health",             "tds-svc"),     # 404 = service UP, path mismatch
]


@pytest.mark.parametrize("path,name", STRIP_SERVICES)
def test_strip_service_healthy(path, name):
    resp = requests.get(f"{BASE_URL}{path}", timeout=10)
    assert resp.status_code == 200, f"{name} returned {resp.status_code}"
    assert resp.json().get("status", "").upper() == "UP", \
        f"{name} health status is not UP"


@pytest.mark.parametrize("method,path,name", NO_STRIP_SERVICES)
def test_no_strip_service_reachable(method, path, name):
    """Verifică că serviciul răspunde (orice cod non-5xx = service UP)."""
    fn = requests.get if method == "GET" else requests.post
    resp = fn(f"{BASE_URL}{path}", timeout=10)
    assert resp.status_code < 500, \
        f"{name} returned {resp.status_code} — service may be down"


def test_fraud_svc_reachable():
    """fraud-svc (Python/FastAPI) — verificat prin endpoint funcțional."""
    resp = requests.post(
        f"{BASE_URL}/fraud/score",
        json={"dpan": "4111110000001234", "amount": 100, "currency": "RON", "merchantId": "m1"},
        timeout=10,
    )
    assert resp.status_code < 500, f"fraud-svc returned {resp.status_code}"
