package ro.minipay.network.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.minipay.network.dto.AuthorizeRequest;
import ro.minipay.network.dto.AuthorizeResponse;
import ro.minipay.network.service.NetworkService;

import java.util.Map;

/**
 * Card network REST API.
 *
 * Called by gateway-svc — not exposed to external clients.
 *
 * POST /network/authorize  — route authorization request to issuer
 */
@RestController
@RequestMapping("/network")
@RequiredArgsConstructor
public class NetworkController {

    private final NetworkService networkService;

    @PostMapping("/authorize")
    public ResponseEntity<AuthorizeResponse> authorize(@Valid @RequestBody AuthorizeRequest request) {
        return ResponseEntity.ok(networkService.authorize(request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
