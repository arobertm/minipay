#!/bin/bash
# MiniPay — test all 4 new services
# Usage: bash scripts/test-services.sh

set -e

AUTH="10.43.6.102:8081"
GW="10.43.171.38:8084"
NOTIF="10.43.135.138:8093"
SETTLE="10.43.213.94:8094"
PSD2="10.43.205.86:8095"
TDS="10.43.251.141:8096"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
ok()   { echo -e "${GREEN}✓ $1${NC}"; }
info() { echo -e "${YELLOW}→ $1${NC}"; }
fail() { echo -e "${RED}✗ $1${NC}"; }
sep()  { echo -e "\n${YELLOW}═══════════════════════════════${NC}"; echo -e "${YELLOW} $1${NC}"; echo -e "${YELLOW}═══════════════════════════════${NC}"; }

# ── 1. tds-svc ────────────────────────────────────────────────────────────────
sep "1. tds-svc — 3DS2 Authentication"

info "Frictionless auth (fraudScore=0.1) → expect transStatus=Y"
RESULT=$(curl -sf -X POST http://$TDS/3ds2/authenticate \
  -H "Content-Type: application/json" \
  -d '{"acctNumber":"4111111111111111","purchaseAmount":5000,"purchaseCurrency":"RON","merchantId":"MERCH-001","fraudScore":0.1}')
echo "$RESULT" | python3 -m json.tool
STATUS=$(echo "$RESULT" | python3 -c "import sys,json; print(json.load(sys.stdin)['transStatus'])")
[[ "$STATUS" == "Y" ]] && ok "transStatus=Y (frictionless)" || fail "Expected Y got $STATUS"

info "Challenge auth (fraudScore=0.85) → expect transStatus=C + OTP"
RESULT=$(curl -sf -X POST http://$TDS/3ds2/authenticate \
  -H "Content-Type: application/json" \
  -d '{"acctNumber":"4111111111111111","purchaseAmount":50000,"purchaseCurrency":"RON","merchantId":"MERCH-001","fraudScore":0.85}')
echo "$RESULT" | python3 -m json.tool
ACS=$(echo "$RESULT" | python3 -c "import sys,json; print(json.load(sys.stdin)['acsTransID'])")
STATUS=$(echo "$RESULT" | python3 -c "import sys,json; print(json.load(sys.stdin)['transStatus'])")
[[ "$STATUS" == "C" ]] && ok "transStatus=C (challenge issued)" || fail "Expected C got $STATUS"

info "Reading OTP for challenge session..."
CHALLENGE=$(curl -sf http://$TDS/3ds2/challenge/$ACS)
echo "$CHALLENGE" | python3 -m json.tool
OTP=$(echo "$CHALLENGE" | python3 -c "import sys,json; print(json.load(sys.stdin)['otp_demo_only'])")
ok "OTP generated: $OTP"

info "Submitting OTP → expect transStatus=Y"
RESULT=$(curl -sf -X POST http://$TDS/3ds2/challenge/$ACS \
  -H "Content-Type: application/json" \
  -d "{\"otp\":\"$OTP\"}")
echo "$RESULT" | python3 -m json.tool
STATUS=$(echo "$RESULT" | python3 -c "import sys,json; print(json.load(sys.stdin)['transStatus'])")
[[ "$STATUS" == "Y" ]] && ok "Challenge completed — transStatus=Y" || fail "Expected Y got $STATUS"

# ── 2. psd2-svc ───────────────────────────────────────────────────────────────
sep "2. psd2-svc — Open Banking AIS + PIS"

info "Creating AIS consent..."
CONSENT_RESP=$(curl -sf -X POST http://$PSD2/psd2/consents \
  -H "Content-Type: application/json" \
  -d '{"psuId":"user-001","accountIds":["ACC-001","ACC-002"],"permissions":["ReadAccountList","ReadBalances","ReadTransactions"]}')
echo "$CONSENT_RESP" | python3 -m json.tool
CONSENT=$(echo "$CONSENT_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['consentId'])")
ok "Consent created: $CONSENT"

info "Listing accounts (AIS)..."
curl -sf http://$PSD2/psd2/accounts -H "Consent-ID: $CONSENT" | python3 -m json.tool
ok "Accounts listed"

info "Getting balances for ACC-001..."
curl -sf http://$PSD2/psd2/accounts/ACC-001/balances -H "Consent-ID: $CONSENT" | python3 -m json.tool
ok "Balance retrieved"

info "Getting transactions for ACC-001..."
curl -sf http://$PSD2/psd2/accounts/ACC-001/transactions -H "Consent-ID: $CONSENT" | python3 -m json.tool
ok "Transactions retrieved"

info "Initiating SEPA Credit Transfer (PIS)..."
PAY_RESP=$(curl -sf -X POST http://$PSD2/psd2/payments/sepa-credit-transfers \
  -H "Content-Type: application/json" \
  -d '{"debtorIban":"RO49AAAA1B31007593840000","creditorIban":"RO49AAAA1B31007593840001","creditorName":"Test SRL","amount":10000,"currency":"RON","remittanceInfo":"invoice-001"}')
echo "$PAY_RESP" | python3 -m json.tool
PAY_ID=$(echo "$PAY_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['paymentId'])")
ok "Payment initiated: $PAY_ID"

info "Authorising payment → expect status=ACSC..."
RESULT=$(curl -sf -X POST http://$PSD2/psd2/payments/sepa-credit-transfers/$PAY_ID/authorise)
echo "$RESULT" | python3 -m json.tool
PAY_STATUS=$(echo "$RESULT" | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])")
[[ "$PAY_STATUS" == "ACSC" ]] && ok "Payment settled — status=ACSC" || fail "Expected ACSC got $PAY_STATUS"

# ── 3. notif-svc + settlement-svc (via gateway) ───────────────────────────────
sep "3. notif-svc + settlement-svc (via real payment)"

info "Getting OAuth2 token..."
TOKEN=$(curl -sf -X POST http://$AUTH/oauth2/token \
  -u "demo-client:demo-secret" \
  -d "grant_type=client_credentials&scope=payments:write" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
ok "Token: ${TOKEN:0:40}..."

info "Authorizing payment via gateway..."
AUTH_RESP=$(curl -sf -X POST http://$GW/gateway/payments/authorize \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"cardNumber":"4111111111111111","amount":2500,"currency":"RON","merchantId":"MERCH-001"}')
echo "$AUTH_RESP" | python3 -m json.tool
TXN=$(echo "$AUTH_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('txnId',''))")
ok "Authorized txnId: $TXN"

info "Capturing payment (triggers Kafka event)..."
curl -sf -X POST http://$GW/gateway/payments/$TXN/capture \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
ok "Captured"

info "Waiting 3s for Kafka consumers to process..."
sleep 3

info "Checking notif-svc stats..."
STATS=$(curl -sf http://$NOTIF/notifications/stats)
echo "$STATS" | python3 -m json.tool
TOTAL=$(echo "$STATS" | python3 -c "import sys,json; print(json.load(sys.stdin)['total'])")
[[ "$TOTAL" -gt 0 ]] && ok "Notifications received: total=$TOTAL" || fail "No notifications yet (Kafka may be slow)"

info "Checking notification for txnId=$TXN..."
curl -sf http://$NOTIF/notifications/$TXN | python3 -m json.tool

info "Checking settlement records..."
RECORDS=$(curl -sf http://$SETTLE/settlements/records)
echo "$RECORDS" | python3 -m json.tool

info "Running on-demand reconciliation for today..."
curl -sf -X POST "http://$SETTLE/settlements/reconcile?date=$(date +%Y-%m-%d)" \
  | python3 -m json.tool
ok "Reconciliation done"

sep "ALL TESTS COMPLETE"
