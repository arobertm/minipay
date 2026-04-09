package ro.minipay.vault.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import ro.minipay.vault.shared.minids.MiniDSClientService;
import ro.minipay.vault.shared.minids.MiniDSClientService.Entry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for vault token entries in MiniDS.
 *
 * DIT:         ou=vault,dc=minipay,dc=ro
 * DN format:   uid={dpan},ou=vault,dc=minipay,dc=ro
 * ObjectClass: vaultToken
 *
 * Stored attributes:
 *   uid         — DPAN (token, 16 digits)
 *   encPan      — AES-256-GCM encrypted PAN (Base64)
 *   encExpiry   — AES-256-GCM encrypted expiry (Base64, may be empty)
 *   bin         — first 6 digits of original PAN (routing, stored in clear)
 *   createdAt   — ISO 8601 timestamp
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VaultTokenRepository {

    static final String VAULT_BASE   = "ou=vault,dc=minipay,dc=ro";
    static final String OBJECT_CLASS = "vaultToken";

    private final MiniDSClientService minids;

    public Entry save(String dpan, String encPan, String encExpiry, String bin, String createdAt) {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("uid",       dpan);
        attrs.put("encPan",    encPan);
        attrs.put("encExpiry", encExpiry != null ? encExpiry : "");
        attrs.put("bin",       bin);
        attrs.put("createdAt", createdAt);

        return minids.createEntry(new Entry(dn(dpan), OBJECT_CLASS, attrs));
    }

    public Optional<Entry> findByDpan(String dpan) {
        return minids.getEntry(dn(dpan));
    }

    public boolean existsByDpan(String dpan) {
        return minids.getEntry(dn(dpan)).isPresent();
    }

    public void delete(String dpan) {
        minids.deleteEntry(dn(dpan));
        log.info("Vault token deleted: {}", dpan);
    }

    private String dn(String dpan) {
        return "uid=" + dpan + "," + VAULT_BASE;
    }
}
