package ro.minipay.issuer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * ISO 8583-style authorization request received from network-svc.
 *
 * @param pan        original card number (detokenized by network-svc)
 * @param expiryDate card expiry MM/YY
 * @param amount     transaction amount in cents
 * @param currency   ISO 4217 currency code (e.g. RON, EUR)
 * @param merchantId merchant identifier
 * @param txnId      gateway transaction ID (for idempotency/logging)
 */
public record AuthorizeRequest(
    @NotBlank String pan,
    String expiryDate,
    @NotNull @Min(1) Long amount,
    @NotBlank String currency,
    @NotBlank String merchantId,
    @NotBlank String txnId
) {}
