import urllib.request
import urllib.error
import json
import base64
import sys

def http_post(url, data, headers=None, timeout=15):
    req = urllib.request.Request(url, data=data.encode('utf-8') if isinstance(data, str) else data, headers=headers or {}, method='POST')
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, resp.read().decode('utf-8')
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8')
    except Exception as ex:
        return None, str(ex)

def http_get(url, headers=None, timeout=15):
    req = urllib.request.Request(url, headers=headers or {}, method='GET')
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, resp.read().decode('utf-8')
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8')
    except Exception as ex:
        return None, str(ex)

# Step 1: Get auth token
print("=== STEP 1: Get Auth Token ===")
creds = base64.b64encode(b'demo-client:demo-secret').decode()
status, body = http_post(
    'http://localhost:8081/oauth2/token',
    'grant_type=client_credentials&scope=payments:write',
    headers={
        'Authorization': f'Basic {creds}',
        'Content-Type': 'application/x-www-form-urlencoded'
    }
)
print(f"HTTP Status: {status}")
print(f"Body: {body}")
access_token = None
if status == 200:
    try:
        token_data = json.loads(body)
        access_token = token_data.get('access_token')
        print(f"Token extracted: {access_token[:40] if access_token else 'NONE'}...")
    except Exception as e:
        print(f"Parse error: {e}")

# Step 2: Run payment
print("\n=== STEP 2: Run Payment ===")
if access_token:
    payment_body = json.dumps({
        "merchantId": "merch-001",
        "orderId": "ORD-DIAG-1",
        "amount": 15000,
        "currency": "RON",
        "pan": "4111111111111111",
        "expiryDate": "12/28",
        "cvv": "123"
    })
    status2, body2 = http_post(
        'http://localhost:8084/v1/payments/authorize',
        payment_body,
        headers={
            'Authorization': f'Bearer {access_token}',
            'Content-Type': 'application/json'
        }
    )
    print(f"HTTP Status: {status2}")
    print(f"Body: {body2}")
else:
    print("SKIPPED: no access token")

# Step 3: Fraud-svc health
print("\n=== STEP 3: Fraud-svc Health ===")
status3, body3 = http_get('http://localhost:8090/health')
print(f"HTTP Status: {status3}")
print(f"Body: {body3}")

# Step 4: Test fraud directly
print("\n=== STEP 4: Fraud Score ===")
fraud_body = json.dumps({
    "dpan": "4111111111111111",
    "amount": 15000,
    "currency": "RON",
    "merchantId": "merch-001",
    "ipAddress": ""
})
status4, body4 = http_post(
    'http://localhost:8090/fraud/score',
    fraud_body,
    headers={'Content-Type': 'application/json'}
)
print(f"HTTP Status: {status4}")
print(f"Body: {body4}")

# Step 5: Audit verify
print("\n=== STEP 5: Audit Verify ===")
status5, body5 = http_get('http://localhost:8091/audit/verify')
print(f"HTTP Status: {status5}")
print(f"Body: {body5}")
