package ro.minipay.network.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ro.minipay.network.client.IssuerClient;
import ro.minipay.network.client.VaultClient;
import ro.minipay.network.client.VaultClient.DetokenizeResult;
import ro.minipay.network.dto.AuthorizeRequest;
import ro.minipay.network.dto.AuthorizeResponse;

/**
 * Card network routing and authorization service.
 *
 * Simulates the Visa/Mastercard network layer:
 *   1. Receive DPAN from gateway
 *   2. Detokenize DPAN → PAN via vault-svc  (EMV network detokenization)
 *   3. Route to issuer-svc based on BIN      (first 6 digits)
 *   4. Return issuer response to gateway
 *
 * In a real network:
 *   - BIN routing determines which issuer bank to contact
 *   - The network signs the authorization request (MAC)
 *   - Full ISO 8583 message format is used
 *
 * For the simulator, BIN routing is simplified to a single issuer-svc instance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkService {

    private final VaultClient  vaultClient;
    private final IssuerClient issuerClient;

    public AuthorizeResponse authorize(AuthorizeRequest request) {
        log.info("Network routing: txnId={} dpan={}**** amount={} {}",
            request.txnId(),
            request.dpan().substring(0, Math.min(6, request.dpan().length())),
            request.amount(), request.currency());

        // Step 1 — Detokenize: DPAN → PAN
        DetokenizeResult detokenized = vaultClient.detokenize(request.dpan());
        if (detokenized == null || detokenized.pan() == null) {
            log.error("Detokenization failed for txnId={}", request.txnId());
            return AuthorizeResponse.systemError();
        }

        // Step 2 — Route to issuer (BIN-based routing — single issuer for simulator)
        String bin = detokenized.pan().substring(0, Math.min(6, detokenized.pan().length()));
        log.debug("BIN routing: bin={} → issuer-svc", bin);

        // Use expiry from vault if not provided in request (vault is authoritative)
        String expiryDate = (request.expiryDate() != null && !request.expiryDate().isBlank())
            ? request.expiryDate()
            : detokenized.expiryDate();

        return issuerClient.authorize(
            detokenized.pan(), expiryDate,
            request.amount(), request.currency(),
            request.merchantId(), request.txnId()
        );
    }
}
