package ro.minipay.vault.dto;

/**
 * Response containing the original PAN (decrypted from vault).
 * This endpoint is restricted to authorized services only (issuer-svc, network-svc).
 */
public record DetokenizeResponse(
    String pan,
    String expiryDate,
    String dpan
) {}
