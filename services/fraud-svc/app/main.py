from fastapi import FastAPI
from prometheus_client import make_asgi_app

app = FastAPI(title="MiniPay Fraud Service", version="1.0.0")

# Prometheus metrics endpoint
metrics_app = make_asgi_app()
app.mount("/metrics", metrics_app)


@app.get("/health")
def health():
    return {"status": "UP", "service": "fraud-svc"}


@app.post("/fraud/score")
def score_transaction(payload: dict):
    """
    Placeholder — va fi inlocuit cu ML model + SHAP
    """
    return {
        "transaction_id": payload.get("transaction_id"),
        "fraud_score": 0.05,
        "decision": "ALLOW",
        "reasons": []
    }
