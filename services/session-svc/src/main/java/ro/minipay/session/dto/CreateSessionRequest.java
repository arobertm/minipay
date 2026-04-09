package ro.minipay.session.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to create a new device session.
 * Typically called by auth-svc after a successful OAuth2 token issuance.
 */
public record CreateSessionRequest(
    @NotBlank String userId,
    String deviceId,
    String ipAddress,
    String userAgent,
    Long ttlSeconds
) {}
