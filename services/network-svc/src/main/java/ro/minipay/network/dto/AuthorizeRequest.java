package ro.minipay.network.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Authorization request received from gateway-svc.
 * Contains DPAN (tokenized card number) — the original PAN is never sent here.
 */
public record AuthorizeRequest(
    @NotBlank String dpan,
    String expiryDate,
    @NotNull @Min(1) Long amount,
    @NotBlank String currency,
    @NotBlank String merchantId,
    @NotBlank String txnId
) {}
