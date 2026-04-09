package ro.minipay.vault.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ro.minipay.vault.crypto.AesGcmCipher;
import ro.minipay.vault.crypto.DpanGenerator;
import ro.minipay.vault.dto.DetokenizeResponse;
import ro.minipay.vault.dto.TokenizeRequest;
import ro.minipay.vault.dto.TokenizeResponse;
import ro.minipay.vault.repository.VaultTokenRepository;
import ro.minipay.vault.shared.minids.MiniDSClientService.Entry;

import java.time.Instant;
import java.util.Map;

/**
 * EMV tokenization service — PAN ↔ DPAN conversion.
 *
 * Tokenize flow:
 *   1. Validate PAN format (13-19 digits)
 *   2. Generate DPAN: preserve BIN (6 digits) + random 9 + Luhn check digit
 *   3. Encrypt PAN with AES-256-GCM (in-memory key)
 *   4. Store encrypted PAN + DPAN in MiniDS (ou=vault)
 *   5. Return DPAN — original PAN never leaves this service
 *
 * Detokenize flow (restricted to authorized callers):
 *   1. Look up DPAN in MiniDS
 *   2. Decrypt PAN with AES-256-GCM
 *   3. Return original PAN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VaultService {

    private final AesGcmCipher      cipher;
    private final DpanGenerator     dpanGenerator;
    private final VaultTokenRepository tokenRepository;

    /**
     * Tokenize a PAN — returns a DPAN.
     * Idempotent: same PAN always gets a new DPAN (tokens are single-use capable).
     */
    public TokenizeResponse tokenize(TokenizeRequest request) {
        String pan = request.pan().replaceAll("\\s+", "");

        // Generate unique DPAN (retry if collision — extremely unlikely)
        String dpan;
        int attempts = 0;
        do {
            dpan = dpanGenerator.generate(pan);
            attempts++;
            if (attempts > 10) throw new IllegalStateException("DPAN generation failed after 10 attempts");
        } while (tokenRepository.existsByDpan(dpan));

        String encPan    = cipher.encrypt(pan);
        String encExpiry = request.expiryDate() != null ? cipher.encrypt(request.expiryDate()) : "";
        String bin       = pan.substring(0, 6);
        String createdAt = Instant.now().toString();

        tokenRepository.save(dpan, encPan, encExpiry, bin, createdAt);

        log.info("PAN tokenized → DPAN: {}****{} (BIN: {})", dpan.substring(0, 6), dpan.substring(12), bin);

        return new TokenizeResponse(dpan, bin, dpan.substring(12), createdAt);
    }

    /**
     * Detokenize a DPAN — returns original PAN.
     * Restricted endpoint: only issuer-svc and network-svc should call this.
     */
    public DetokenizeResponse detokenize(String dpan) {
        Entry entry = tokenRepository.findByDpan(dpan)
            .orElseThrow(() -> new TokenNotFoundException(dpan));

        Map<String, String> a = entry.getAttributes();
        String pan     = cipher.decrypt(a.get("encPan"));
        String encExp  = a.getOrDefault("encExpiry", "");
        String expiry  = encExp.isEmpty() ? null : cipher.decrypt(encExp);

        log.info("DPAN detokenized: {}****{}", dpan.substring(0, 6), dpan.substring(12));
        return new DetokenizeResponse(pan, expiry, dpan);
    }

    /**
     * Delete a token (GDPR erasure or card lifecycle end).
     */
    public void deleteToken(String dpan) {
        tokenRepository.findByDpan(dpan)
            .orElseThrow(() -> new TokenNotFoundException(dpan));
        tokenRepository.delete(dpan);
        log.info("Vault token deleted: {}", dpan);
    }

    // --- exceptions ---

    public static class TokenNotFoundException extends RuntimeException {
        public TokenNotFoundException(String dpan) {
            super("Token not found: " + dpan);
        }
    }
}
