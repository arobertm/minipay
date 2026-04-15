package ro.minipay.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ro.minipay.gateway.client.FraudClient;
import ro.minipay.gateway.client.FraudClient.FraudResult;
import ro.minipay.gateway.client.NetworkClient;
import ro.minipay.gateway.client.NetworkClient.AuthorizationResult;
import ro.minipay.gateway.client.VaultClient;
import ro.minipay.gateway.dto.AuthorizeRequest;
import ro.minipay.gateway.dto.CaptureRequest;
import ro.minipay.gateway.dto.PaymentResponse;
import ro.minipay.gateway.dto.RefundRequest;
import ro.minipay.gateway.kafka.PaymentAuditEvent;
import ro.minipay.gateway.kafka.PaymentEventPublisher;
import ro.minipay.gateway.model.PaymentDeclineReason;
import ro.minipay.gateway.model.PaymentStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core payment orchestration service.
 *
 * Authorization flow:
 *   1. Tokenize PAN via vault-svc  (PAN never stored here)
 *   2. Score fraud via fraud-svc   (SHAP explanation included)
 *   3. Block if score >= 0.8
 *   4. Flag CHALLENGE if score >= 0.5 (3DS2 — future integration)
 *   5. Authorize via network-svc → issuer-svc
 *   6. Store result in-memory (TODO: persist to MiniDS or PostgreSQL)
 *
 * Note: in-memory store is sufficient for demo/dissertation purposes.
 * Production would use a persistent store with idempotency keys.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final VaultClient            vaultClient;
    private final FraudClient            fraudClient;
    private final NetworkClient          networkClient;
    private final PaymentEventPublisher  eventPublisher;

    // In-memory store: txnId → PaymentResponse
    // Sufficient for demo; production would use MiniDS or PostgreSQL
    private final Map<String, PaymentResponse> store = new ConcurrentHashMap<>();

    /**
     * Authorize a payment — full orchestration flow.
     */
    public PaymentResponse authorize(AuthorizeRequest request, String clientIp) {
        String txnId = UUID.randomUUID().toString();
        log.info("Authorizing payment txnId={} amount={} {} merchant={}",
            txnId, request.amount(), request.currency(), request.merchantId());

        // Step 1 — Tokenize PAN (PAN goes to vault, never stored here)
        String dpan;
        try {
            dpan = vaultClient.tokenize(request.pan(), request.expiryDate());
        } catch (Exception e) {
            log.error("Tokenization failed for txnId={}: {}", txnId, e.getMessage());
            return save(txnId, null, request, PaymentStatus.ERROR,
                null, 0.0, null, null, PaymentDeclineReason.INTERNAL_ERROR);
        }

        // Step 2 — Fraud scoring
        FraudResult fraud = fraudClient.score(dpan, request.amount(),
            request.currency(), request.merchantId(), clientIp);

        // Step 3 — Block high-risk transactions
        if (fraud.isBlocked()) {
            log.warn("Payment BLOCKED by fraud engine: txnId={} score={}", txnId, fraud.score());
            return save(txnId, dpan, request, PaymentStatus.BLOCKED,
                null, fraud.score(), fraud.reasons(), null, PaymentDeclineReason.FRAUD_BLOCKED);
        }

        // Step 4 — Flag for 3DS2 challenge (medium risk)
        if (fraud.needsChallenge()) {
            log.info("Payment flagged for 3DS2 challenge: txnId={} score={}", txnId, fraud.score());
            // TODO: integrate tds-svc for OTP challenge flow
            // For now, return CHALLENGE status — merchant should redirect user to 3DS
            return save(txnId, dpan, request, PaymentStatus.CHALLENGE,
                null, fraud.score(), fraud.reasons(), null, null);
        }

        // Step 5 — Send to network (ISO 8583)
        AuthorizationResult netResult = networkClient.authorize(
            dpan, request.expiryDate(), request.amount(),
            request.currency(), request.merchantId(), txnId);

        PaymentStatus status = netResult.isApproved() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED;
        PaymentDeclineReason declineReason = netResult.isApproved() ? null
            : mapIsoCode(netResult.responseCode());

        log.info("Payment {}: txnId={} isoCode={} authCode={}",
            status, txnId, netResult.responseCode(), netResult.authCode());

        return save(txnId, dpan, request, status, netResult.responseCode(),
            fraud.score(), fraud.reasons(), netResult.authCode(), declineReason);
    }

    /**
     * Capture a previously authorized payment.
     */
    public PaymentResponse capture(String txnId, CaptureRequest request) {
        PaymentResponse existing = getOrThrow(txnId);

        if (existing.status() != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException(
                "Cannot capture payment in status: " + existing.status());
        }

        PaymentResponse captured = new PaymentResponse(
            txnId, existing.dpan(), PaymentStatus.CAPTURED,
            "00", request.amount(),
            request.currency() != null ? request.currency() : existing.currency(),
            existing.merchantId(), existing.orderId(),
            existing.fraudScore(), existing.fraudReasons(),
            existing.authCode(), null, Instant.now().toString()
        );
        store.put(txnId, captured);
        log.info("Payment CAPTURED: txnId={} amount={}", txnId, request.amount());
        return captured;
    }

    /**
     * Refund a captured payment.
     */
    public PaymentResponse refund(String txnId, RefundRequest request) {
        PaymentResponse existing = getOrThrow(txnId);

        if (existing.status() != PaymentStatus.CAPTURED) {
            throw new IllegalStateException(
                "Cannot refund payment in status: " + existing.status());
        }

        PaymentResponse refunded = new PaymentResponse(
            txnId, existing.dpan(), PaymentStatus.REFUNDED,
            "00", request.amount(), existing.currency(),
            existing.merchantId(), existing.orderId(),
            existing.fraudScore(), existing.fraudReasons(),
            existing.authCode(), null, Instant.now().toString()
        );
        store.put(txnId, refunded);
        log.info("Payment REFUNDED: txnId={} amount={} reason={}",
            txnId, request.amount(), request.reason());
        return refunded;
    }

    /**
     * Get payment status by transaction ID.
     */
    public PaymentResponse getPayment(String txnId) {
        return getOrThrow(txnId);
    }

    // --- helpers ---

    private PaymentResponse save(String txnId, String dpan, AuthorizeRequest req,
                                  PaymentStatus status, String isoCode,
                                  double fraudScore, java.util.List<String> reasons,
                                  String authCode, PaymentDeclineReason declineReason) {
        String timestamp = Instant.now().toString();
        PaymentResponse response = new PaymentResponse(
            txnId, dpan, status, isoCode,
            req.amount(), req.currency(), req.merchantId(), req.orderId(),
            fraudScore, reasons, authCode, declineReason, timestamp
        );
        store.put(txnId, response);

        // Publish to Kafka → audit-svc will append to hash chain
        eventPublisher.publish(new PaymentAuditEvent(
            txnId, status.name(), req.amount(), req.currency(),
            req.merchantId(), "", fraudScore, timestamp
        ));

        return response;
    }

    private PaymentResponse getOrThrow(String txnId) {
        PaymentResponse r = store.get(txnId);
        if (r == null) throw new PaymentNotFoundException(txnId);
        return r;
    }

    private PaymentDeclineReason mapIsoCode(String code) {
        return switch (code) {
            case "51" -> PaymentDeclineReason.INSUFFICIENT_FUNDS;
            case "14" -> PaymentDeclineReason.INVALID_CARD;
            case "54" -> PaymentDeclineReason.EXPIRED_CARD;
            case "05" -> PaymentDeclineReason.DO_NOT_HONOR;
            case "65" -> PaymentDeclineReason.ACTIVITY_LIMIT_EXCEEDED;
            default   -> PaymentDeclineReason.ISSUER_UNAVAILABLE;
        };
    }

    // --- exceptions ---

    public static class PaymentNotFoundException extends RuntimeException {
        public PaymentNotFoundException(String txnId) {
            super("Payment not found: " + txnId);
        }
    }
}
