package ro.minipay.audit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.minipay.audit.kafka.PaymentAuditEvent;
import ro.minipay.audit.model.AuditEntry;
import ro.minipay.audit.repository.AuditEntryRepository;

import java.util.List;

/**
 * Audit service — appends payment events to the immutable hash chain.
 *
 * Append flow:
 *   1. Load latest entry to get prevHash (or GENESIS_HASH if chain is empty)
 *   2. Build new AuditEntry with next sequenceNumber
 *   3. Compute entryHash = SHA-256(data + prevHash)
 *   4. Save to PostgreSQL
 *
 * Verify flow:
 *   1. Load all entries ordered by sequenceNumber ASC
 *   2. For each entry: recompute hash, compare with stored hash
 *   3. Return first broken entry (if any)
 *
 * The append method is synchronized to prevent race conditions when multiple
 * Kafka messages arrive concurrently (hash chain must be strictly sequential).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEntryRepository repository;
    private final HashChainService     hashChain;

    /**
     * Append a payment event to the hash chain.
     * Idempotent: duplicate txnIds are silently skipped.
     */
    @Transactional
    public synchronized AuditEntry append(PaymentAuditEvent event) {
        // Idempotency check — skip duplicates
        if (repository.findByTxnId(event.txnId()).isPresent()) {
            log.debug("Duplicate event skipped: txnId={}", event.txnId());
            return repository.findByTxnId(event.txnId()).get();
        }

        // Get prevHash from latest entry (or genesis)
        String prevHash = repository.findTopByOrderBySequenceNumberDesc()
            .map(AuditEntry::getEntryHash)
            .orElse(HashChainService.GENESIS_HASH);

        long nextSeq = repository.count() + 1;

        AuditEntry entry = AuditEntry.builder()
            .sequenceNumber(nextSeq)
            .txnId(event.txnId())
            .status(event.status())
            .amount(event.amount())
            .currency(event.currency())
            .merchantId(event.merchantId())
            .fraudScore(event.fraudScore())
            .eventTimestamp(event.timestamp() != null ? event.timestamp() : java.time.Instant.now().toString())
            .prevHash(prevHash)
            .entryHash("") // placeholder — computed below
            .build();

        // Compute hash with all fields set
        String hash = hashChain.computeHash(entry);
        entry.setEntryHash(hash);

        AuditEntry saved = repository.save(entry);
        log.info("Audit entry #{}: txnId={} status={} hash={}...",
            nextSeq, event.txnId(), event.status(), hash.substring(0, 12));
        return saved;
    }

    /**
     * Verify the integrity of the entire hash chain.
     * Returns a VerifyResult with isValid=true if no tampering is detected.
     */
    @Transactional(readOnly = true)
    public VerifyResult verify() {
        List<AuditEntry> entries = repository.findAllByOrderBySequenceNumberAsc();

        if (entries.isEmpty()) {
            return new VerifyResult(true, entries.size(), null, "Chain is empty");
        }

        for (AuditEntry entry : entries) {
            if (!hashChain.verify(entry)) {
                log.warn("CHAIN INTEGRITY VIOLATION at seq={} txnId={}",
                    entry.getSequenceNumber(), entry.getTxnId());
                return new VerifyResult(false, entries.size(), entry,
                    "Hash mismatch at sequence #" + entry.getSequenceNumber()
                    + " (txnId=" + entry.getTxnId() + ")");
            }
        }

        log.info("Hash chain verified: {} entries, all intact.", entries.size());
        return new VerifyResult(true, entries.size(), null,
            "Chain intact — " + entries.size() + " entries verified");
    }

    /** Paginated list of entries (most recent first). */
    @Transactional(readOnly = true)
    public Page<AuditEntry> getEntries(int page, int size) {
        return repository.findAllByOrderBySequenceNumberDesc(PageRequest.of(page, size));
    }

    /** Find a single entry by transaction ID. */
    @Transactional(readOnly = true)
    public java.util.Optional<AuditEntry> getByTxnId(String txnId) {
        return repository.findByTxnId(txnId);
    }

    public record VerifyResult(
        boolean isValid,
        int     totalEntries,
        AuditEntry brokenEntry,
        String  message
    ) {}
}
