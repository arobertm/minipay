package ro.minipay.psd2.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.minipay.psd2.model.Consent;
import ro.minipay.psd2.service.ConsentService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * PSD2 AIS — Consent management.
 *
 * POST /psd2/consents                    — create consent
 * GET  /psd2/consents/{consentId}        — get consent status
 * DELETE /psd2/consents/{consentId}      — revoke consent
 */
@RestController
@RequestMapping("/psd2/consents")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;

    @PostMapping
    public ResponseEntity<Consent> create(@RequestBody Map<String, Object> body) {
        String psuId = (String) body.getOrDefault("psuId", "unknown-psu");
        @SuppressWarnings("unchecked")
        List<String> accountIds = (List<String>) body.getOrDefault("accountIds", List.of());
        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) body.getOrDefault("permissions",
                List.of("ReadAccountList", "ReadBalances", "ReadTransactions"));
        String validUntilStr = (String) body.getOrDefault("validUntil",
                LocalDate.now().plusDays(90).toString());
        LocalDate validUntil = LocalDate.parse(validUntilStr);

        Consent consent = consentService.create(psuId, accountIds, permissions, validUntil);
        return ResponseEntity.ok(consent);
    }

    @GetMapping("/{consentId}")
    public ResponseEntity<Consent> get(@PathVariable String consentId) {
        return consentService.get(consentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{consentId}")
    public ResponseEntity<Map<String, String>> revoke(@PathVariable String consentId) {
        boolean revoked = consentService.revoke(consentId);
        if (!revoked) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("consentId", consentId, "status", "REVOKED"));
    }
}
