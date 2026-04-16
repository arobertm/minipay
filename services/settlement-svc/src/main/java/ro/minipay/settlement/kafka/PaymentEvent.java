package ro.minipay.settlement.kafka;

/**
 * Payment event consumed from Kafka topic "payment-events".
 * Published by gateway-svc after every payment operation.
 */
public record PaymentEvent(
    String txnId,
    String status,
    Long   amount,
    String currency,
    String merchantId,
    String ipAddress,
    double fraudScore,
    String timestamp
) {}
