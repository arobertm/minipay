package ro.minipay.tds.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ro.minipay.tds.model.AuthenticationRequest;
import ro.minipay.tds.model.AuthenticationResult;
import ro.minipay.tds.model.ChallengeSession;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 3DS2 authentication engine.
 *
 * Risk-based decisioning:
 *   fraudScore < 0.3  → frictionless Y (no challenge)
 *   fraudScore < 0.7  → frictionless A (attempted, no liability shift)
 *   fraudScore >= 0.7 → challenge C (OTP required)
 *
 * In a production ACS this would integrate with the issuing bank's
 * customer database and send a real OTP via SMS/authenticator app.
 */
@Slf4j
@Service
public class ThreeDSService {

    @Value("${tds.base-url:http://localhost:8096}")
    private String baseUrl;

    private final Map<String, ChallengeSession> challenges = new ConcurrentHashMap<>();

    public AuthenticationResult authenticate(AuthenticationRequest req) {
        String acsTransId     = UUID.randomUUID().toString();
        String serverTransId  = req.threeDSServerTransID() != null
                ? req.threeDSServerTransID() : UUID.randomUUID().toString();

        if (req.fraudScore() >= 0.7) {
            return issueChallenge(acsTransId, serverTransId, req);
        } else if (req.fraudScore() >= 0.3) {
            return frictionless(acsTransId, serverTransId, "A", "06", "Attempted");
        } else {
            return frictionless(acsTransId, serverTransId, "Y", "05", "Full Authentication");
        }
    }

    /**
     * Verify OTP for a challenge session.
     * Returns the final authentication result on success.
     */
    public Optional<AuthenticationResult> verifyChallenge(String acsTransId, String submittedOtp) {
        ChallengeSession session = challenges.get(acsTransId);
        if (session == null) return Optional.empty();

        if (Instant.now().isAfter(session.expiresAt())) {
            challenges.put(acsTransId, expiredSession(session));
            log.warn("[3DS2] challenge expired acsTransId={}", acsTransId);
            return Optional.of(failedResult(acsTransId, session.threeDSServerTransID(), "Challenge Expired"));
        }

        if (!session.otp().equals(submittedOtp)) {
            log.warn("[3DS2] wrong OTP acsTransId={}", acsTransId);
            return Optional.of(failedResult(acsTransId, session.threeDSServerTransID(), "Wrong OTP"));
        }

        challenges.remove(acsTransId);
        log.info("[3DS2] challenge verified acsTransId={}", acsTransId);
        return Optional.of(new AuthenticationResult(
                acsTransId, session.threeDSServerTransID(),
                "Y", "Challenge Successful",
                generateCavv(acsTransId), "05",
                null, Instant.now()));
    }

    public Optional<ChallengeSession> getChallenge(String acsTransId) {
        return Optional.ofNullable(challenges.get(acsTransId));
    }

    // ── Private ────────────────────────────────────────────────────────────────

    private AuthenticationResult frictionless(String acsId, String svrId,
                                              String status, String eci, String reason) {
        log.info("[3DS2] frictionless {} acsTransId={}", status, acsId);
        return new AuthenticationResult(acsId, svrId, status, reason,
                generateCavv(acsId), eci, null, Instant.now());
    }

    private AuthenticationResult issueChallenge(String acsId, String svrId,
                                                AuthenticationRequest req) {
        String otp = String.format("%06d", (int)(Math.random() * 1_000_000));
        ChallengeSession session = new ChallengeSession(
                acsId, svrId, req.acctNumber(), otp, "PENDING",
                Instant.now(), Instant.now().plus(10, ChronoUnit.MINUTES));
        challenges.put(acsId, session);
        String challengeUrl = baseUrl + "/3ds2/challenge/" + acsId;

        log.info("[3DS2] challenge issued acsTransId={} otp={} (demo only — never log OTP in prod)",
                acsId, otp);
        return new AuthenticationResult(acsId, svrId, "C", "Challenge Required",
                null, null, challengeUrl, Instant.now());
    }

    private AuthenticationResult failedResult(String acsId, String svrId, String reason) {
        return new AuthenticationResult(acsId, svrId, "N", reason,
                null, "07", null, Instant.now());
    }

    private ChallengeSession expiredSession(ChallengeSession s) {
        return new ChallengeSession(s.acsTransID(), s.threeDSServerTransID(),
                s.acctNumber(), s.otp(), "EXPIRED", s.createdAt(), s.expiresAt());
    }

    /** Simulate a 28-byte CAVV (Cardholder Authentication Verification Value). */
    private String generateCavv(String seed) {
        byte[] bytes = (seed + Instant.now().toString()).substring(0, 21).getBytes();
        return Base64.getEncoder().encodeToString(bytes).substring(0, 28);
    }
}
