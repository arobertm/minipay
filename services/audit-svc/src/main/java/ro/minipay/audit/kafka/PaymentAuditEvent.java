package ro.minipay.audit.kafka;

/**
 * Payment event consumed from Kafka topic "payment-events".
 * Must match the structure published by gateway-svc.
 */
public record PaymentAuditEvent(
    String txnId,
    String status,
    Long   amount,
    String currency,
    String merchantId,
    String ipAddress,
    double fraudScore,
    String timestamp
) {}
