"""
XGBoost model wrapper with SHAP explainability.

Lifecycle:
  1. On startup: try to load model from MODEL_PATH
  2. If not found: train on synthetic data, save to MODEL_PATH
  3. On each predict: run XGBoost + SHAP TreeExplainer
  4. Return score (0-1) + top-3 SHAP reasons in Romanian

SHAP (SHapley Additive exPlanations) assigns each feature a contribution
to the prediction. Positive SHAP = pushes toward fraud; negative = toward legit.
Reference: Lundberg & Lee 2017 — "A Unified Approach to Interpreting Model Predictions"
"""

import os
import logging
import pickle
from pathlib import Path

import numpy as np
import shap
import xgboost as xgb

from app.features import FEATURE_NAMES, FEATURE_DESCRIPTIONS
from app.schemas import ShapDetail
from app.training import train_model

logger = logging.getLogger(__name__)

MODEL_DIR  = Path(os.getenv("MODEL_DIR", "/app/model_store"))
MODEL_PATH = MODEL_DIR / "fraud_xgb.pkl"
EXPLAINER_PATH = MODEL_DIR / "shap_explainer.pkl"


class FraudModel:
    def __init__(self):
        self._model: xgb.XGBClassifier | None = None
        self._explainer: shap.TreeExplainer | None = None

    def load_or_train(self) -> None:
        MODEL_DIR.mkdir(parents=True, exist_ok=True)

        if MODEL_PATH.exists():
            logger.info("Loading existing model from %s", MODEL_PATH)
            with open(MODEL_PATH, "rb") as f:
                self._model = pickle.load(f)
            with open(EXPLAINER_PATH, "rb") as f:
                self._explainer = pickle.load(f)
            logger.info("Model loaded successfully.")
        else:
            logger.info("No model found — training on synthetic data...")
            self._model = train_model()
            self._explainer = shap.TreeExplainer(self._model)

            with open(MODEL_PATH, "wb") as f:
                pickle.dump(self._model, f)
            with open(EXPLAINER_PATH, "wb") as f:
                pickle.dump(self._explainer, f)
            logger.info("Model saved to %s", MODEL_PATH)

    def predict(self, feature_vector: list[float]) -> tuple[float, list[ShapDetail]]:
        """
        Run XGBoost prediction + SHAP explanation.

        Returns:
            score       — fraud probability 0.0-1.0
            shap_details — top features by |SHAP value| with descriptions
        """
        X = np.array([feature_vector], dtype=np.float32)

        # Fraud probability
        score = float(self._model.predict_proba(X)[0, 1])

        # SHAP explanation
        shap_values = self._explainer.shap_values(X)[0]  # shape: (n_features,)

        # Build ShapDetail list sorted by absolute contribution (descending)
        details = []
        for i, (name, sv) in enumerate(zip(FEATURE_NAMES, shap_values)):
            low_desc, high_desc = FEATURE_DESCRIPTIONS[name]
            # Positive SHAP → feature pushed toward fraud → use high_desc
            description = high_desc if sv > 0 else low_desc
            details.append(ShapDetail(
                feature=name,
                shap_value=round(float(sv), 4),
                description=description,
            ))

        details.sort(key=lambda d: abs(d.shap_value), reverse=True)
        return score, details

    def is_ready(self) -> bool:
        return self._model is not None


# Singleton — loaded once at startup
fraud_model = FraudModel()
