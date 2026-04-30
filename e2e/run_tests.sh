#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

echo "=== MiniPay E2E Test Suite — api-minipay.online ==="
echo ""

# Instalare dependențe dacă lipsesc
if ! python -c "import pytest" &>/dev/null; then
  echo "[setup] Instalare dependențe..."
  pip install -r requirements.txt -q
fi

# Rulare teste cu raport HTML
python -m pytest \
  test_01_health.py \
  test_02_auth.py \
  test_03_payment_happy_path.py \
  test_04_payment_declined.py \
  test_05_fraud.py \
  test_06_vault.py \
  test_07_audit.py \
  test_08_tds.py \
  test_09_settlement_psd2.py \
  test_10_notifications.py \
  -v \
  --tb=short \
  --html=report.html \
  --self-contained-html \
  "$@"

echo ""
echo "Raport HTML: e2e/report.html"
