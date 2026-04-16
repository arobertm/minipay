package ro.minipay.tds.model;

import java.time.Instant;

/**
 * Active 3DS2 challenge session awaiting OTP verification.
 */
public record ChallengeSession(
    String  acsTransID,
    String  threeDSServerTransID,
    String  acctNumber,
    String  otp,           // 6-digit simulated OTP
    String  status,        // PENDING | COMPLETED | EXPIRED
    Instant createdAt,
    Instant expiresAt
) {}
