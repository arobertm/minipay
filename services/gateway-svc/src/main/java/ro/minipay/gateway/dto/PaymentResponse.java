package ro.minipay.gateway.dto;

import ro.minipay.gateway.model.PaymentDeclineReason;
import ro.minipay.gateway.model.PaymentStatus;

import java.util.List;

/**
 * Unified response for all payment operations.
 *
 * txnId          — MiniPay transaction ID (UUID)
 * dpan           — tokenized card (PAN never returned)
 * status         — AUTHORIZED / DECLINED / BLOCKED / CHALLENGE
 * isoResponseCode — ISO 8583 response code (00=OK, 51=insuf, etc.)
 * fraudScore     — 0.0-1.0 from fraud-svc
 * fraudReasons   — SHAP explanation (list of top features)
 * authCode       — authorization code from issuer (if approved)
 */
public record PaymentResponse(
    String txnId,
    String dpan,
    PaymentStatus status,
    String isoResponseCode,
    Long amount,
    String currency,
    String merchantId,
    String orderId,
    Double fraudScore,
    List<String> fraudReasons,
    String authCode,
    PaymentDeclineReason declineReason,
    String processedAt
) {}
