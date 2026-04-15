package ro.minipay.audit.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.minipay.audit.model.AuditEntry;
import ro.minipay.audit.service.AuditService;
import ro.minipay.audit.service.AuditService.VerifyResult;

import java.util.Map;

/**
 * Audit REST API.
 *
 * GET  /audit/entries              — paginated audit log (most recent first)
 * GET  /audit/entries/{txnId}      — single entry by transaction ID
 * GET  /audit/verify               — verify integrity of entire hash chain
 * POST /audit/tamper-demo/{txnId}  — DEMO ONLY: simulates tampering for dissertation demo
 */
@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/entries")
    public ResponseEntity<Page<AuditEntry>> getEntries(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.getEntries(page, size));
    }

    @GetMapping("/entries/{txnId}")
    public ResponseEntity<?> getByTxnId(@PathVariable String txnId) {
        return auditService.getByTxnId(txnId)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElse(ResponseEntity.status(404).body(Map.of("error", "Entry not found: " + txnId)));
    }

    /**
     * Verify the integrity of the entire hash chain.
     *
     * Returns:
     *   isValid=true  — all entries intact, no tampering detected
     *   isValid=false — chain broken at sequenceNumber X (tampered entry details included)
     *
     * This is the key demo for the dissertation:
     *   1. Process some payments → chain grows
     *   2. Manually UPDATE an entry in PostgreSQL
     *   3. Call /audit/verify → detects tampering instantly
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify() {
        VerifyResult result = auditService.verify();

        Map<String, Object> body;
        if (result.isValid()) {
            body = Map.of(
                "isValid",      true,
                "totalEntries", result.totalEntries(),
                "message",      result.message()
            );
        } else {
            AuditEntry broken = result.brokenEntry();
            body = Map.of(
                "isValid",        false,
                "totalEntries",   result.totalEntries(),
                "message",        result.message(),
                "brokenSequence", broken.getSequenceNumber(),
                "brokenTxnId",    broken.getTxnId()
            );
        }

        return ResponseEntity.ok(body);
    }
}
