package ro.minipay.issuer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.minipay.issuer.dto.AuthorizeRequest;
import ro.minipay.issuer.dto.AuthorizeResponse;
import ro.minipay.issuer.service.IssuerService;

import java.util.Map;

/**
 * Issuer bank REST API.
 *
 * Called internally by network-svc only — not exposed to external clients.
 *
 * POST /issuer/authorize  — process an authorization request
 * GET  /issuer/cards      — list test cards (debug only)
 */
@RestController
@RequestMapping("/issuer")
@RequiredArgsConstructor
public class IssuerController {

    private final IssuerService issuerService;

    @PostMapping("/authorize")
    public ResponseEntity<AuthorizeResponse> authorize(@Valid @RequestBody AuthorizeRequest request) {
        return ResponseEntity.ok(issuerService.authorize(request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
