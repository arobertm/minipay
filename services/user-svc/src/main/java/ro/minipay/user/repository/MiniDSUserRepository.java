package ro.minipay.user.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import ro.minipay.user.model.UserRole;
import ro.minipay.user.model.UserStatus;
import ro.minipay.user.shared.minids.MiniDSClientService;
import ro.minipay.user.shared.minids.MiniDSClientService.Entry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for user entries in MiniDS.
 *
 * DIT location: ou=users,dc=minipay,dc=ro
 * DN format:    uid={userId},ou=users,dc=minipay,dc=ro
 * ObjectClass:  person (MiniDS built-in)
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MiniDSUserRepository {

    static final String USERS_BASE = "ou=users,dc=minipay,dc=ro";
    static final String OBJECT_CLASS = "person";

    private final MiniDSClientService minids;

    public Entry save(String userId, String email, String passwordHash,
                      String firstName, String lastName,
                      UserRole role, UserStatus status, String createdAt) {

        Map<String, String> attrs = new HashMap<>();
        attrs.put("uid", userId);
        attrs.put("mail", email);
        attrs.put("userPassword", passwordHash);
        attrs.put("givenName", firstName);
        attrs.put("sn", lastName);
        attrs.put("role", role.name());
        attrs.put("status", status.name());
        attrs.put("createdAt", createdAt);

        Entry entry = new Entry(dn(userId), OBJECT_CLASS, attrs);
        return minids.createEntry(entry);
    }

    public Optional<Entry> findById(String userId) {
        return minids.getEntry(dn(userId));
    }

    public Optional<Entry> findByEmail(String email) {
        List<Entry> results = minids.search(USERS_BASE, Map.of("mail", email), 1);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public void update(String userId, Map<String, String> updates) {
        Map<String, String> attrs = new HashMap<>(updates);
        Entry patch = new Entry(dn(userId), OBJECT_CLASS, attrs);
        minids.patchEntry(dn(userId), patch);
    }

    public void delete(String userId) {
        minids.deleteEntry(dn(userId));
    }

    public boolean existsByEmail(String email) {
        return !minids.search(USERS_BASE, Map.of("mail", email), 1).isEmpty();
    }

    private String dn(String userId) {
        return "uid=" + userId + "," + USERS_BASE;
    }
}
