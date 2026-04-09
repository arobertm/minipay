package ro.minipay.gateway.dto;

import jakarta.validation.constraints.*;

/**
 * Payment authorization request from merchant.
 *
 * pan        — card number (13-19 digits), never stored in gateway
 * expiryDate — MMYY format
 * cvv        — 3-4 digits, never stored anywhere
 * amount     — in cents (e.g. 10000 = 100.00 RON)
 * currency   — ISO 4217 (RON, EUR, USD)
 * merchantId — identifies the merchant
 * orderId    — merchant's own order reference
 */
public record AuthorizeRequest(
    @NotBlank
    @Pattern(regexp = "\\d{13,19}", message = "Invalid PAN format")
    String pan,

    @NotBlank
    @Pattern(regexp = "\\d{4}", message = "expiryDate must be MMYY")
    String expiryDate,

    @NotBlank
    @Pattern(regexp = "\\d{3,4}", message = "CVV must be 3-4 digits")
    String cvv,

    @NotNull @Min(1)
    Long amount,

    @NotBlank @Size(min = 3, max = 3)
    String currency,

    @NotBlank
    String merchantId,

    @NotBlank
    String orderId,

    String description
) {}
