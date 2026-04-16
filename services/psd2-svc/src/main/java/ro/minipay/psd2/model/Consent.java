package ro.minipay.psd2.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * PSD2 account access consent (AIS).
 * Issued when a TPP requests access to a PSU's accounts.
 */
public record Consent(
    String consentId,
    String psuId,
    List<String> accountIds,
    List<String> permissions,   // "ReadAccountList", "ReadBalances", "ReadTransactions"
    LocalDate validUntil,
    String status,              // RECEIVED → VALID → EXPIRED / REVOKED
    Instant createdAt
) {}
