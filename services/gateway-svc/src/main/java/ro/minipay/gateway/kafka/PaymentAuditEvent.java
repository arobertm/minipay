package ro.minipay.gateway.kafka;

/**
 * Event published to Kafka topic "payment-events" after every payment operation.
 * Consumed by audit-svc to build the immutable hash chain audit log.
 *
 * @param txnId      gateway transaction ID
 * @param status     AUTHORIZED | DECLINED | BLOCKED | CHALLENGE | CAPTURED | REFUNDED | ERROR
 * @param amount     amount in cents
 * @param currency   ISO 4217 (RON, EUR, ...)
 * @param merchantId merchant identifier
 * @param ipAddress  client IP (for fraud correlation)
 * @param fraudScore fraud score from fraud-svc (0.0-1.0)
 * @param timestamp  ISO-8601 timestamp
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
