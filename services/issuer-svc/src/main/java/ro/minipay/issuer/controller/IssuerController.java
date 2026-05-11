package ro.minipay.issuer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.minipay.issuer.config.DataLoader;
import ro.minipay.issuer.dto.AuthorizeRequest;
import ro.minipay.issuer.dto.AuthorizeResponse;
import ro.minipay.issuer.service.IssuerService;

import java.util.Map;

/**
 * Issuer bank REST API.
 *
 * Called internally by network-svc only — not exposed to external clients.
 *
 * POST /issuer/authorize        — process an authorization request
 * POST /issuer/admin/reset-cards — reset test card balances to initial values
 */
@RestController
@RequestMapping("/issuer")
@RequiredArgsConstructor
public class IssuerController {

    private final IssuerService issuerService;
    private final DataLoader dataLoader;

    @PostMapping("/authorize")
    public ResponseEntity<AuthorizeResponse> authorize(@Valid @RequestBody AuthorizeRequest request) {
        return ResponseEntity.ok(issuerService.authorize(request));
    }

    @PostMapping("/admin/reset-cards")
    public ResponseEntity<Map<String, String>> resetCards() throws Exception {
        dataLoader.run(new DefaultApplicationArguments());
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Test card balances reset to initial values."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
