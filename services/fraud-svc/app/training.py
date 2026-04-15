"""
Synthetic dataset generation and XGBoost model training.

Since we cannot use real payment data for a dissertation project,
we generate a synthetic dataset that mimics realistic fraud patterns
based on known industry heuristics:

  - Large amounts at night + foreign currency → high fraud probability
  - Very large amounts (> 5000 RON) from external IPs → high fraud probability
  - Weekend + external IP + large amount → medium-high fraud probability
  - ~2% random baseline fraud rate (accounts for hard-to-detect fraud)

Dataset size: ~10 000 transactions (8500 legit + 1500 fraud)
Class imbalance is handled via XGBoost scale_pos_weight.

Reference: "XGBoost: A Scalable Tree Boosting System" — Chen & Guestrin 2016
           "A Unified Approach to Interpreting Model Predictions" — Lundberg & Lee 2017
"""

import math
import random
import logging

import numpy as np
import pandas as pd
import xgboost as xgb
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, roc_auc_score

from app.features import FEATURE_NAMES

logger = logging.getLogger(__name__)

N_LEGIT  = 8500
N_FRAUD  = 1500
RANDOM_STATE = 42


def _generate_legit() -> list[dict]:
    """Normal transactions: business hours, RON, small-medium amounts, private IPs."""
    rows = []
    for _ in range(N_LEGIT):
        hour    = random.choices(range(24), weights=_hour_weights_legit())[0]
        weekday = random.randint(0, 6)
        amount  = random.randint(100, 150_000)      # 1 RON – 1500 RON
        currency_risk = random.choices([0.0, 0.4, 0.8], weights=[85, 12, 3])[0]
        ip_ext  = random.choices([0.0, 1.0], weights=[90, 10])[0]
        amount_large  = 1.0 if amount > 200_000 else 0.0
        is_night      = 1.0 if hour < 6 else 0.0
        is_weekend    = 1.0 if weekday >= 5 else 0.0

        rows.append({
            "amount_log1p":   math.log1p(amount / 100.0),
            "is_night":       is_night,
            "is_weekend":     is_weekend,
            "currency_risk":  currency_risk,
            "amount_large":   amount_large,
            "ip_external":    ip_ext,
            "hour_of_day":    float(hour),
            "amount_cents_k": amount / 100_000.0,
            "label": 0,
        })
    return rows


def _generate_fraud() -> list[dict]:
    """
    Fraudulent transactions: mix of known fraud patterns.

    Pattern 1 (40%) — Card testing: small amounts, external IP, unusual hours
    Pattern 2 (35%) — Account takeover: large amounts, night, foreign currency
    Pattern 3 (25%) — Merchant fraud: very large amounts, unknown merchant profile
    """
    rows = []

    # Pattern 1 — Card testing (small amounts, validate card is alive)
    for _ in range(int(N_FRAUD * 0.40)):
        hour   = random.choice([1, 2, 3, 4, 23, 0])
        amount = random.randint(100, 5_000)     # 1–50 RON
        rows.append({
            "amount_log1p":   math.log1p(amount / 100.0),
            "is_night":       1.0,
            "is_weekend":     float(random.randint(0, 1)),
            "currency_risk":  random.choice([0.0, 0.4]),
            "amount_large":   0.0,
            "ip_external":    1.0,
            "hour_of_day":    float(hour),
            "amount_cents_k": amount / 100_000.0,
            "label": 1,
        })

    # Pattern 2 — Account takeover (large, night, foreign currency)
    for _ in range(int(N_FRAUD * 0.35)):
        hour   = random.choice([0, 1, 2, 3, 4, 5])
        amount = random.randint(200_000, 800_000)    # 2000–8000 RON
        rows.append({
            "amount_log1p":   math.log1p(amount / 100.0),
            "is_night":       1.0,
            "is_weekend":     float(random.randint(0, 1)),
            "currency_risk":  random.choice([0.4, 0.8]),
            "amount_large":   1.0,
            "ip_external":    1.0,
            "hour_of_day":    float(hour),
            "amount_cents_k": amount / 100_000.0,
            "label": 1,
        })

    # Pattern 3 — Merchant fraud (very large, external IP, any hour)
    for _ in range(int(N_FRAUD * 0.25)):
        hour   = random.randint(0, 23)
        amount = random.randint(500_000, 2_000_000)  # 5000–20000 RON
        rows.append({
            "amount_log1p":   math.log1p(amount / 100.0),
            "is_night":       1.0 if hour < 6 else 0.0,
            "is_weekend":     float(random.randint(0, 1)),
            "currency_risk":  random.choice([0.0, 0.4, 0.8]),
            "amount_large":   1.0,
            "ip_external":    1.0,
            "hour_of_day":    float(hour),
            "amount_cents_k": amount / 100_000.0,
            "label": 1,
        })

    return rows


def _hour_weights_legit() -> list[float]:
    """Business-hour biased distribution for legitimate transactions."""
    weights = []
    for h in range(24):
        if 9 <= h <= 20:
            weights.append(8.0)
        elif 7 <= h <= 22:
            weights.append(4.0)
        else:
            weights.append(0.5)
    return weights


def train_model() -> xgb.XGBClassifier:
    """
    Generate synthetic dataset and train XGBoost binary classifier.
    Returns the trained model.
    """
    random.seed(RANDOM_STATE)
    np.random.seed(RANDOM_STATE)

    logger.info("Generating synthetic fraud dataset (%d legit, %d fraud)...",
                N_LEGIT, N_FRAUD)

    rows = _generate_legit() + _generate_fraud()
    random.shuffle(rows)

    df = pd.DataFrame(rows)
    X = df[FEATURE_NAMES].values
    y = df["label"].values

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=RANDOM_STATE, stratify=y
    )

    # scale_pos_weight compensates for class imbalance (legit >> fraud)
    pos_weight = (y_train == 0).sum() / (y_train == 1).sum()

    model = xgb.XGBClassifier(
        n_estimators=200,
        max_depth=5,
        learning_rate=0.05,
        subsample=0.8,
        colsample_bytree=0.8,
        scale_pos_weight=pos_weight,
        eval_metric="auc",
        random_state=RANDOM_STATE,
        verbosity=0,
    )

    model.fit(X_train, y_train)

    # Evaluate and log metrics
    y_pred  = model.predict(X_test)
    y_proba = model.predict_proba(X_test)[:, 1]
    auc     = roc_auc_score(y_test, y_proba)

    logger.info("Model trained — AUC=%.4f", auc)
    logger.info("\n%s", classification_report(y_test, y_pred,
                                              target_names=["legit", "fraud"]))

    return model
