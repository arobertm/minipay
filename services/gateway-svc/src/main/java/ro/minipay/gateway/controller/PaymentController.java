package ro.minipay.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.minipay.gateway.dto.AuthorizeRequest;
import ro.minipay.gateway.dto.CaptureRequest;
import ro.minipay.gateway.dto.PaymentResponse;
import ro.minipay.gateway.dto.RefundRequest;
import ro.minipay.gateway.service.PaymentService;
import ro.minipay.gateway.service.PaymentService.PaymentNotFoundException;

import java.util.Map;

/**
 * Payment API — entry point for merchants.
 *
 * POST  /v1/payments/authorize          — authorize a payment
 * POST  /v1/payments/{txnId}/capture    — capture after authorize
 * POST  /v1/payments/{txnId}/refund     — refund a captured payment
 * GET   /v1/payments/{txnId}            — get payment status
 */
@Slf4j
@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/authorize")
    public ResponseEntity<PaymentResponse> authorize(
            @Valid @RequestBody AuthorizeRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = resolveClientIp(httpRequest);
        PaymentResponse response = paymentService.authorize(request, clientIp);

        // 201 Created for new authorized/blocked/challenge payments
        // 200 OK for declined (not a server error — issuer decision)
        HttpStatus status = switch (response.status()) {
            case AUTHORIZED, BLOCKED, CHALLENGE -> HttpStatus.CREATED;
            default -> HttpStatus.OK;
        };
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/{txnId}/capture")
    public ResponseEntity<PaymentResponse> capture(
            @PathVariable String txnId,
            @Valid @RequestBody CaptureRequest request) {

        return ResponseEntity.ok(paymentService.capture(txnId, request));
    }

    @PostMapping("/{txnId}/refund")
    public ResponseEntity<PaymentResponse> refund(
            @PathVariable String txnId,
            @Valid @RequestBody RefundRequest request) {

        return ResponseEntity.ok(paymentService.refund(txnId, request));
    }

    @GetMapping("/{txnId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String txnId) {
        return ResponseEntity.ok(paymentService.getPayment(txnId));
    }

    // --- exception handlers ---

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(PaymentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleInvalidState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", ex.getMessage()));
    }

    // --- helpers ---

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
