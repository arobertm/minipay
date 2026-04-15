from pydantic import BaseModel


class ScoreRequest(BaseModel):
    dpan: str
    amount: int          # in cents (e.g. 10000 = 100.00 RON)
    currency: str = "RON"
    merchantId: str
    ipAddress: str = ""


class ShapDetail(BaseModel):
    feature: str
    shap_value: float
    description: str


class ScoreResponse(BaseModel):
    score: float          # 0.0 – 1.0
    decision: str         # ALLOW | CHALLENGE | BLOCK
    reasons: list[str]    # human-readable list (for GDPR Art.22 explanation)
    shap_details: list[ShapDetail]
