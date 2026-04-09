package ro.minipay.vault.dto;

/**
 * Response containing the DPAN (Device/Digital PAN — the token).
 *
 * dpan       — 16-digit Luhn-valid token (same BIN as original PAN)
 * bin        — first 6 digits of original PAN (preserved in DPAN)
 * last4      — last 4 digits of DPAN (for display purposes)
 * createdAt  — ISO 8601 timestamp
 */
public record TokenizeResponse(
    String dpan,
    String bin,
    String last4,
    String createdAt
) {}
