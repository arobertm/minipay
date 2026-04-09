package ro.minipay.gateway.model;

public enum PaymentStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    DECLINED,
    BLOCKED,       // fraud score > 0.8
    CHALLENGE,     // 3DS2 challenge required
    REFUNDED,
    ERROR
}
