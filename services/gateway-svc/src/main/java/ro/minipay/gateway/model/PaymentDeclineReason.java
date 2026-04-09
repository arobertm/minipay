package ro.minipay.gateway.model;

public enum PaymentDeclineReason {
    FRAUD_BLOCKED,
    INSUFFICIENT_FUNDS,
    INVALID_CARD,
    EXPIRED_CARD,
    DO_NOT_HONOR,
    ACTIVITY_LIMIT_EXCEEDED,
    ISSUER_UNAVAILABLE,
    INTERNAL_ERROR
}
