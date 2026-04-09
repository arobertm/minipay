package ro.minipay.gateway.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Capture a previously authorized payment.
 * Amount can be <= authorized amount (partial capture).
 */
public record CaptureRequest(
    @NotNull @Min(1)
    Long amount,

    String currency
) {}
