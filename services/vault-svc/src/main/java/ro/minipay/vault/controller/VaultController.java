package ro.minipay.vault.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.minipay.vault.dto.DetokenizeResponse;
import ro.minipay.vault.dto.TokenizeRequest;
import ro.minipay.vault.dto.TokenizeResponse;
import ro.minipay.vault.service.VaultService;
import ro.minipay.vault.service.VaultService.TokenNotFoundException;

import java.util.Map;

/**
 * REST API for EMV tokenization.
 *
 * POST  /vault/tokenize              — PAN → DPAN (requires JWT)
 * POST  /vault/detokenize/{dpan}     — DPAN → PAN (restricted)
 * DELETE /vault/tokens/{dpan}        — delete token (GDPR / card lifecycle)
 */
@Slf4j
@RestController
@RequestMapping("/vault")
@RequiredArgsConstructor
public class VaultController {

    private final VaultService vaultService;

    /**
     * Tokenize a PAN.
     * Called by gateway-svc when a new card is registered.
     */
    @PostMapping("/tokenize")
    public ResponseEntity<TokenizeResponse> tokenize(@Valid @RequestBody TokenizeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vaultService.tokenize(request));
    }

    /**
     * Detokenize a DPAN — retrieve the original PAN.
     * Restricted to authorized services (issuer-svc, network-svc) via JWT scope.
     */
    @PostMapping("/detokenize/{dpan}")
    public ResponseEntity<DetokenizeResponse> detokenize(@PathVariable String dpan) {
        return ResponseEntity.ok(vaultService.detokenize(dpan));
    }

    /**
     * Delete a vault token.
     * Used for GDPR erasure or when a card is deactivated.
     */
    @DeleteMapping("/tokens/{dpan}")
    public ResponseEntity<Void> deleteToken(@PathVariable String dpan) {
        vaultService.deleteToken(dpan);
        return ResponseEntity.noContent().build();
    }

    // --- exception handlers ---

    @ExceptionHandler(TokenNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(TokenNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", ex.getMessage()));
    }
}
