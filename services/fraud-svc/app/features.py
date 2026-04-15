"""
Feature engineering for fraud detection.

Features extracted from each transaction request:
  1. amount_log1p     — log1p(amount / 100) — reduces skew of large amounts
  2. is_night         — 1 if hour is 0-5 (high-risk window)
  3. is_weekend       — 1 if Saturday or Sunday
  4. currency_risk    — 0.0=RON, 0.4=EUR/USD/GBP, 0.8=other
  5. amount_large     — 1 if amount > 200000 cents (> 2000 RON)
  6. ip_external      — 1 if IP is not private/loopback
  7. hour_of_day      — 0-23 (raw hour for non-linear patterns)
  8. amount_cents_k   — amount / 100000 (normalized, 0-100 range)

These must be kept in sync with training (features.py is the single source of truth).
"""

import math
import ipaddress
from datetime import datetime, timezone


FEATURE_NAMES = [
    "amount_log1p",
    "is_night",
    "is_weekend",
    "currency_risk",
    "amount_large",
    "ip_external",
    "hour_of_day",
    "amount_cents_k",
]

# Human-readable descriptions for SHAP explanations
# Maps feature name -> (low_desc, high_desc)
FEATURE_DESCRIPTIONS = {
    "amount_log1p":    ("Suma normala",              "Suma neobisnuit de mare"),
    "is_night":        ("Ora normala de zi",          "Tranzactie in timpul noptii (00:00-05:59)"),
    "is_weekend":      ("Zi lucratoare",              "Tranzactie in weekend"),
    "currency_risk":   ("Moneda locala (RON)",        "Moneda straina (risc ridicat)"),
    "amount_large":    ("Suma sub pragul de risc",    "Suma depaseste 2000 RON"),
    "ip_external":     ("IP local/privat",            "Adresa IP externa necunoscuta"),
    "hour_of_day":     ("Ora tranzactiei normala",    "Ora tranzactiei suspect"),
    "amount_cents_k":  ("Suma normala",               "Suma foarte mare"),
}


def _currency_risk(currency: str) -> float:
    c = currency.upper()
    if c == "RON":
        return 0.0
    if c in ("EUR", "USD", "GBP", "CHF"):
        return 0.4
    return 0.8


def _ip_external(ip: str) -> float:
    if not ip:
        return 0.0
    try:
        addr = ipaddress.ip_address(ip)
        if addr.is_private or addr.is_loopback or addr.is_link_local:
            return 0.0
        return 1.0
    except ValueError:
        return 0.0


def extract(amount: int, currency: str, ip: str) -> list[float]:
    """
    Extract feature vector from a transaction.
    Returns a list in the same order as FEATURE_NAMES.
    """
    now = datetime.now(timezone.utc)
    hour = now.hour
    weekday = now.weekday()  # 0=Mon … 6=Sun

    amount_log1p  = math.log1p(amount / 100.0)
    is_night      = 1.0 if hour < 6 else 0.0
    is_weekend    = 1.0 if weekday >= 5 else 0.0
    curr_risk     = _currency_risk(currency)
    amount_large  = 1.0 if amount > 200_000 else 0.0   # > 2000 RON
    ip_ext        = _ip_external(ip)
    hour_f        = float(hour)
    amount_cents_k = amount / 100_000.0

    return [
        amount_log1p,
        is_night,
        is_weekend,
        curr_risk,
        amount_large,
        ip_ext,
        hour_f,
        amount_cents_k,
    ]
