package ro.minipay.vault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request to tokenize a PAN (Primary Account Number).
 *
 * pan        — 13-19 digit card number
 * expiryDate — MMYY format (optional, stored encrypted alongside PAN)
 * requestId  — idempotency key (optional)
 */
public record TokenizeRequest(
    @NotBlank
    @Pattern(regexp = "\\d{13,19}", message = "PAN must be 13-19 digits")
    String pan,

    @Pattern(regexp = "\\d{4}", message = "expiryDate must be MMYY format")
    String expiryDate,

    String requestId
) {}
