"""
MiniPay Fraud Detection Service

FastAPI service that scores payment transactions using XGBoost + SHAP.

Endpoints:
  POST /fraud/score  — score a transaction, returns fraud probability + SHAP reasons
  GET  /health       — health check (returns UP/LOADING)
  GET  /metrics      — Prometheus metrics

Decision thresholds:
  score < 0.5  → ALLOW
  score < 0.8  → CHALLENGE (3DS2 recommended)
  score >= 0.8 → BLOCK

GDPR Art.22 compliance: every BLOCK/CHALLENGE decision includes a human-readable
explanation derived from SHAP values, so the cardholder can understand why
their transaction was flagged.
"""

import logging
import time
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from prometheus_client import Counter, Histogram, make_asgi_app

from app.features import extract
from app.model import fraud_model
from app.schemas import ScoreRequest, ScoreResponse

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s — %(message)s",
)
logger = logging.getLogger(__name__)

# ── Prometheus metrics ────────────────────────────────────────────────────────
SCORE_COUNTER = Counter(
    "fraud_score_total",
    "Total fraud scoring requests",
    ["decision"],
)
SCORE_LATENCY = Histogram(
    "fraud_score_latency_seconds",
    "Fraud scoring latency",
    buckets=[0.01, 0.05, 0.1, 0.25, 0.5, 1.0],
)


# ── Lifespan: load/train model at startup ─────────────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("fraud-svc starting — loading model...")
    fraud_model.load_or_train()
    logger.info("fraud-svc ready.")
    yield


# ── FastAPI app ───────────────────────────────────────────────────────────────
app = FastAPI(
    title="MiniPay Fraud Detection Service",
    version="1.0.0",
    description=(
        "XGBoost-based fraud detection with SHAP explainability.\n\n"
        "Decision thresholds: ALLOW < 0.5 | CHALLENGE 0.5-0.8 | BLOCK >= 0.8\n\n"
        "GDPR Art.22: each decision includes human-readable SHAP-based explanation."
    ),
    lifespan=lifespan,
)

# Mount Prometheus metrics at /metrics
metrics_app = make_asgi_app()
app.mount("/metrics", metrics_app)


# ── Endpoints ─────────────────────────────────────────────────────────────────

@app.get("/health")
def health():
    return {
        "status": "UP" if fraud_model.is_ready() else "LOADING",
        "service": "fraud-svc",
        "model": "XGBoost + SHAP",
    }


@app.post("/fraud/score", response_model=ScoreResponse)
def score_transaction(request: ScoreRequest):
    if not fraud_model.is_ready():
        raise HTTPException(status_code=503, detail="Model not ready yet")

    start = time.perf_counter()

    # Extract features from request
    features = extract(
        amount=request.amount,
        currency=request.currency,
        ip=request.ipAddress,
    )

    # Predict
    score, shap_details = fraud_model.predict(features)

    # Decision
    if score >= 0.8:
        decision = "BLOCK"
    elif score >= 0.5:
        decision = "CHALLENGE"
    else:
        decision = "ALLOW"

    # Top-3 SHAP reasons with positive impact (pushed toward fraud)
    # Only include features that actually contributed toward fraud (shap_value > 0)
    fraud_drivers = [d for d in shap_details if d.shap_value > 0][:3]
    reasons = [d.description for d in fraud_drivers] if fraud_drivers else []

    elapsed = time.perf_counter() - start
    SCORE_COUNTER.labels(decision=decision).inc()
    SCORE_LATENCY.observe(elapsed)

    logger.info(
        "fraud/score: dpan=%s**** score=%.4f decision=%s reasons=%s latency=%.3fs",
        request.dpan[:6] if len(request.dpan) >= 6 else request.dpan,
        score, decision, reasons, elapsed,
    )

    return ScoreResponse(
        score=round(score, 4),
        decision=decision,
        reasons=reasons,
        shap_details=shap_details,
    )
