package ro.minipay.audit.service;

import org.springframework.stereotype.Component;
import ro.minipay.audit.model.AuditEntry;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 hash chain implementation for immutable audit logging.
 *
 * Each entry's hash covers all its data fields plus the previous entry's hash,
 * forming a tamper-evident chain. Any modification to any entry makes its
 * stored hash inconsistent with a recomputation, and breaks all subsequent
 * entries (since prevHash propagates the corruption).
 *
 * Genesis hash (first entry): prevHash = 64 zeros.
 *
 * This provides:
 *   - Tamper detection: any modification is detectable
 *   - Ordering proof: entries cannot be reordered without breaking hashes
 *   - Non-repudiation: approved transactions cannot be retroactively denied
 *
 * Relevant standards: PCI DSS v4.0 Req.10 (audit log integrity)
 */
@Component
public class HashChainService {

    public static final String GENESIS_HASH =
        "0000000000000000000000000000000000000000000000000000000000000000";

    /**
     * Compute SHA-256 hash for an audit entry.
     * Input: concatenation of all meaningful fields + prevHash.
     */
    public String computeHash(AuditEntry entry) {
        String input = entry.getSequenceNumber()
            + "|" + entry.getTxnId()
            + "|" + entry.getStatus()
            + "|" + entry.getAmount()
            + "|" + entry.getCurrency()
            + "|" + entry.getMerchantId()
            + "|" + entry.getFraudScore()
            + "|" + entry.getEventTimestamp()
            + "|" + entry.getPrevHash();
        return sha256Hex(input);
    }

    /**
     * Verify the integrity of a single entry against its stored hash.
     */
    public boolean verify(AuditEntry entry) {
        return computeHash(entry).equals(entry.getEntryHash());
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
