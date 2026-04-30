import pytest
import requests

BASE_URL = "https://api-minipay.online"

# OAuth2 client credentials (gateway-svc client registered in auth-svc) demo-client:demo-secret
CLIENT_ID     = "demo-client"
CLIENT_SECRET = "demo-secret"

# Test cards pre-loaded in issuer-svc
CARD_APPROVED      = {"pan": "4111111111111111", "expiry": "12/28", "cvv": "123"}
CARD_BLOCKED       = {"pan": "4000000000000002", "expiry": "12/28", "cvv": "123"}
CARD_INSUF_FUNDS   = {"pan": "4000000000009995", "expiry": "12/28", "cvv": "123"}
CARD_MASTERCARD    = {"pan": "5500000000000004", "expiry": "12/28", "cvv": "123"}


@pytest.fixture(scope="session")
def token():
    """Fetch a Bearer token via client_credentials from auth-svc."""
    resp = requests.post(
        f"{BASE_URL}/auth/oauth2/token",
        data={"grant_type": "client_credentials", "scope": "payments:read payments:write"},
        auth=(CLIENT_ID, CLIENT_SECRET),
        timeout=10,
    )
    assert resp.status_code == 200, f"Token fetch failed: {resp.status_code} {resp.text}"
    return resp.json()["access_token"]


@pytest.fixture(scope="session")
def auth_headers(token):
    return {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
