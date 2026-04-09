package ro.minipay.session.dto;

import ro.minipay.session.model.SessionStatus;

public record SessionResponse(
    String sessionId,
    String userId,
    String deviceId,
    String ipAddress,
    String userAgent,
    String createdAt,
    String expiresAt,
    String lastSeenAt,
    SessionStatus status
) {}
