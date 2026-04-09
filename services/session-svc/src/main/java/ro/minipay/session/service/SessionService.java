package ro.minipay.session.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ro.minipay.session.dto.CreateSessionRequest;
import ro.minipay.session.dto.SessionResponse;
import ro.minipay.session.model.SessionStatus;
import ro.minipay.session.repository.MiniDSSessionRepository;
import ro.minipay.session.shared.minids.MiniDSClientService.Entry;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Business logic for device session management.
 *
 * Sessions are stored in MiniDS under ou=sessions,dc=minipay,dc=ro.
 * Expiry is enforced in code (no DB-level TTL) — expired sessions
 * are auto-revoked on first access.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private static final long DEFAULT_TTL_SECONDS = 3600L; // 1 hour

    private final MiniDSSessionRepository sessionRepository;

    /**
     * Create a new device session (called after successful OAuth2 login).
     */
    public SessionResponse createSession(CreateSessionRequest request) {
        String sessionId = UUID.randomUUID().toString();
        long ttl = request.ttlSeconds() != null ? request.ttlSeconds() : DEFAULT_TTL_SECONDS;
        String now = Instant.now().toString();
        String expiresAt = Instant.now().plusSeconds(ttl).toString();

        sessionRepository.save(
            sessionId, request.userId(), request.deviceId(),
            request.ipAddress(), request.userAgent(),
            now, expiresAt, now, SessionStatus.ACTIVE
        );

        log.info("Session created: {} for userId={}", sessionId, request.userId());
        return new SessionResponse(
            sessionId, request.userId(), request.deviceId(),
            request.ipAddress(), request.userAgent(),
            now, expiresAt, now, SessionStatus.ACTIVE
        );
    }

    /**
     * Get session by ID. Auto-revokes if expired.
     */
    public SessionResponse getSession(String sessionId) {
        Entry entry = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new SessionNotFoundException(sessionId));

        SessionResponse response = toResponse(entry);

        // Auto-revoke if expired
        if (response.status() == SessionStatus.ACTIVE && isExpired(response.expiresAt())) {
            sessionRepository.updateStatus(sessionId, SessionStatus.EXPIRED);
            return new SessionResponse(
                response.sessionId(), response.userId(), response.deviceId(),
                response.ipAddress(), response.userAgent(),
                response.createdAt(), response.expiresAt(), response.lastSeenAt(),
                SessionStatus.EXPIRED
            );
        }

        return response;
    }

    /**
     * List all active sessions for a user.
     */
    public List<SessionResponse> getUserSessions(String userId) {
        return sessionRepository.findActiveByUserId(userId).stream()
            .map(this::toResponse)
            .filter(s -> !isExpired(s.expiresAt()))
            .toList();
    }

    /**
     * Revoke a single session (logout from one device).
     */
    public void revokeSession(String sessionId) {
        sessionRepository.findById(sessionId)
            .orElseThrow(() -> new SessionNotFoundException(sessionId));
        sessionRepository.updateStatus(sessionId, SessionStatus.REVOKED);
        log.info("Session revoked: {}", sessionId);
    }

    /**
     * Revoke all active sessions for a user (logout from all devices / security breach).
     */
    public int revokeAllUserSessions(String userId) {
        List<Entry> active = sessionRepository.findActiveByUserId(userId);
        for (Entry entry : active) {
            String sid = entry.getAttribute("uid");
            if (sid != null) {
                sessionRepository.updateStatus(sid, SessionStatus.REVOKED);
            }
        }
        log.info("Revoked {} sessions for userId={}", active.size(), userId);
        return active.size();
    }

    /**
     * Touch lastSeenAt — called on each authenticated request.
     */
    public void touch(String sessionId) {
        sessionRepository.updateLastSeen(sessionId, Instant.now().toString());
    }

    // --- helpers ---

    private boolean isExpired(String expiresAt) {
        try {
            return Instant.parse(expiresAt).isBefore(Instant.now());
        } catch (Exception e) {
            return false;
        }
    }

    private SessionResponse toResponse(Entry entry) {
        Map<String, String> a = entry.getAttributes();
        return new SessionResponse(
            a.getOrDefault("uid",        ""),
            a.getOrDefault("userId",     ""),
            a.getOrDefault("deviceId",   ""),
            a.getOrDefault("ipAddress",  ""),
            a.getOrDefault("userAgent",  ""),
            a.getOrDefault("createdAt",  ""),
            a.getOrDefault("expiresAt",  ""),
            a.getOrDefault("lastSeenAt", ""),
            parseStatus(a.get("status"))
        );
    }

    private SessionStatus parseStatus(String value) {
        try { return SessionStatus.valueOf(value); }
        catch (Exception e) { return SessionStatus.ACTIVE; }
    }

    // --- exceptions ---

    public static class SessionNotFoundException extends RuntimeException {
        public SessionNotFoundException(String id) {
            super("Session not found: " + id);
        }
    }
}
