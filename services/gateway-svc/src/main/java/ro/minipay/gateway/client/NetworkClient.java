package ro.minipay.gateway.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * REST client for network-svc.
 *
 * Sends ISO 8583-style authorization requests to the card network simulator.
 * network-svc routes to issuer-svc based on BIN (first 6 digits of DPAN).
 *
 * ISO 8583 Response Codes:
 *   00 — Approved
 *   05 — Do Not Honor
 *   14 — Invalid Card Number
 *   51 — Insufficient Funds
 *   54 — Expired Card
 *   65 — Activity Limit Exceeded
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NetworkClient {

    private final RestTemplate restTemplate;

    @Value("${services.network-url}")
    private String networkUrl;

    public AuthorizationResult authorize(String dpan, String expiryDate,
                                          Long amount, String currency,
                                          String merchantId, String txnId) {
        try {
            Map<String, Object> body = Map.of(
                "dpan",       dpan,
                "expiryDate", expiryDate != null ? expiryDate : "",
                "amount",     amount,
                "currency",   currency,
                "merchantId", merchantId,
                "txnId",      txnId
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                networkUrl + "/network/authorize", body, Map.class);

            if (response == null) return AuthorizationResult.error();

            String responseCode = (String) response.getOrDefault("responseCode", "96");
            String authCode     = (String) response.getOrDefault("authCode", "");
            log.info("Network authorization: responseCode={} authCode={} txnId={}",
                responseCode, authCode, txnId);
            return new AuthorizationResult(responseCode, authCode);

        } catch (Exception e) {
            log.error("network-svc unavailable: {}", e.getMessage());
            return AuthorizationResult.error();
        }
    }

    public record AuthorizationResult(String responseCode, String authCode) {
        public static AuthorizationResult error() {
            return new AuthorizationResult("96", ""); // 96 = System Malfunction
        }
        public boolean isApproved() { return "00".equals(responseCode); }
    }
}
