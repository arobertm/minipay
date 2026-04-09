package ro.minipay.gateway.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Refund a captured payment.
 * Amount can be <= captured amount (partial refund).
 */
public record RefundRequest(
    @NotNull @Min(1)
    Long amount,

    String reason
) {}
