package ro.minipay.psd2.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ro.minipay.psd2.model.Consent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages PSD2 AIS consents (in-memory for dissertation demo).
 */
@Slf4j
@Service
public class ConsentService {

    private final Map<String, Consent> store = new ConcurrentHashMap<>();

    public Consent create(String psuId, List<String> accountIds,
                          List<String> permissions, LocalDate validUntil) {
        String consentId = UUID.randomUUID().toString();
        Consent consent = new Consent(
                consentId, psuId, accountIds, permissions,
                validUntil, "VALID", Instant.now());
        store.put(consentId, consent);
        log.info("[PSD2-AIS] consent created id={} psu={} accounts={}", consentId, psuId, accountIds);
        return consent;
    }

    public Optional<Consent> get(String consentId) {
        return Optional.ofNullable(store.get(consentId))
                .map(this::expireIfNeeded);
    }

    public boolean revoke(String consentId) {
        Consent existing = store.get(consentId);
        if (existing == null) return false;
        Consent revoked = new Consent(
                existing.consentId(), existing.psuId(), existing.accountIds(),
                existing.permissions(), existing.validUntil(), "REVOKED", existing.createdAt());
        store.put(consentId, revoked);
        log.info("[PSD2-AIS] consent revoked id={}", consentId);
        return true;
    }

    /**
     * Validates that a consent is VALID and has the required permission.
     */
    public boolean isAuthorized(String consentId, String permission) {
        return get(consentId)
                .filter(c -> "VALID".equals(c.status()))
                .filter(c -> c.permissions().contains(permission))
                .isPresent();
    }

    private Consent expireIfNeeded(Consent c) {
        if ("VALID".equals(c.status()) && LocalDate.now().isAfter(c.validUntil())) {
            Consent expired = new Consent(c.consentId(), c.psuId(), c.accountIds(),
                    c.permissions(), c.validUntil(), "EXPIRED", c.createdAt());
            store.put(c.consentId(), expired);
            return expired;
        }
        return c;
    }
}
