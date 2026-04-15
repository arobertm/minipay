package ro.minipay.audit.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Immutable audit log entry with hash chain integrity.
 *
 * Hash chain structure:
 *   entryHash = SHA-256(sequenceNumber + txnId + status + amount + currency
 *                       + merchantId + fraudScore + timestamp + prevHash)
 *
 * Any modification to a stored entry will cause entryHash to no longer match
 * its recomputed value, and all subsequent entries will also fail verification
 * (since prevHash propagates the corruption forward).
 *
 * This is functionally equivalent to a blockchain but without distributed consensus —
 * appropriate for a single-operator audit log (PCI DSS requirement 10).
 *
 * Reference: PCI DSS v4.0 Requirement 10 — Log and Monitor All Access
 */
@Entity
@Table(name = "audit_entries",
       indexes = @Index(name = "idx_audit_txn_id", columnList = "txnId"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Monotonically increasing sequence — used for ordered chain verification */
    @Column(nullable = false, unique = true)
    private Long sequenceNumber;

    /** Gateway transaction ID */
    @Column(nullable = false, length = 64)
    private String txnId;

    /** Payment status: AUTHORIZED, DECLINED, BLOCKED, CHALLENGE, CAPTURED, REFUNDED, ERROR */
    @Column(nullable = false, length = 20)
    private String status;

    /** Amount in cents */
    @Column(nullable = false)
    private Long amount;

    /** ISO 4217 currency code */
    @Column(nullable = false, length = 10)
    private String currency;

    /** Merchant identifier */
    @Column(nullable = false)
    private String merchantId;

    /** Fraud score from fraud-svc (0.0-1.0) */
    @Column(nullable = false)
    private Double fraudScore;

    /** ISO-8601 timestamp from gateway-svc */
    @Column(nullable = false)
    private String eventTimestamp;

    /** SHA-256 hash of previous entry (genesis entry uses 64 zeros) */
    @Column(nullable = false, length = 64)
    private String prevHash;

    /**
     * SHA-256(sequenceNumber + txnId + status + amount + currency
     *         + merchantId + fraudScore + eventTimestamp + prevHash)
     */
    @Column(nullable = false, length = 64)
    private String entryHash;
}
