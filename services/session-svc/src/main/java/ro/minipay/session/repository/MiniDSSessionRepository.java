package ro.minipay.session.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import ro.minipay.session.model.SessionStatus;
import ro.minipay.session.shared.minids.MiniDSClientService;
import ro.minipay.session.shared.minids.MiniDSClientService.Entry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for session entries in MiniDS.
 *
 * DIT location: ou=sessions,dc=minipay,dc=ro
 * DN format:    uid={sessionId},ou=sessions,dc=minipay,dc=ro
 * ObjectClass:  session
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MiniDSSessionRepository {

    static final String SESSIONS_BASE = "ou=sessions,dc=minipay,dc=ro";
    static final String OBJECT_CLASS  = "session";

    private final MiniDSClientService minids;

    public Entry save(String sessionId, String userId, String deviceId,
                      String ipAddress, String userAgent,
                      String createdAt, String expiresAt, String lastSeenAt,
                      SessionStatus status) {

        Map<String, String> attrs = new HashMap<>();
        attrs.put("uid",         sessionId);
        attrs.put("userId",      userId);
        attrs.put("deviceId",    deviceId != null ? deviceId : "unknown");
        attrs.put("ipAddress",   ipAddress != null ? ipAddress : "");
        attrs.put("userAgent",   userAgent != null ? userAgent : "");
        attrs.put("createdAt",   createdAt);
        attrs.put("expiresAt",   expiresAt);
        attrs.put("lastSeenAt",  lastSeenAt);
        attrs.put("status",      status.name());

        Entry entry = new Entry(dn(sessionId), OBJECT_CLASS, attrs);
        return minids.createEntry(entry);
    }

    public Optional<Entry> findById(String sessionId) {
        return minids.getEntry(dn(sessionId));
    }

    public List<Entry> findByUserId(String userId) {
        return minids.search(SESSIONS_BASE, Map.of("userId", userId), 100);
    }

    public List<Entry> findActiveByUserId(String userId) {
        return minids.search(SESSIONS_BASE,
            Map.of("userId", userId, "status", SessionStatus.ACTIVE.name()), 100);
    }

    public void updateStatus(String sessionId, SessionStatus status) {
        Entry patch = new Entry(dn(sessionId), OBJECT_CLASS, Map.of("status", status.name()));
        minids.patchEntry(dn(sessionId), patch);
    }

    public void updateLastSeen(String sessionId, String lastSeenAt) {
        Entry patch = new Entry(dn(sessionId), OBJECT_CLASS, Map.of("lastSeenAt", lastSeenAt));
        minids.patchEntry(dn(sessionId), patch);
    }

    private String dn(String sessionId) {
        return "uid=" + sessionId + "," + SESSIONS_BASE;
    }
}
