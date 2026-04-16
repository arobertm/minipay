package ro.minipay.psd2.model;

import java.time.Instant;

/**
 * PSD2 SEPA Credit Transfer payment initiation (PIS).
 */
public record PaymentInitiation(
    String paymentId,
    String debtorIban,
    String creditorIban,
    String creditorName,
    Long   amount,       // minor units (cents)
    String currency,
    String remittanceInfo,
    String status,       // RCVD → ACTC → ACSC / RJCT
    Instant createdAt,
    Instant updatedAt
) {}
