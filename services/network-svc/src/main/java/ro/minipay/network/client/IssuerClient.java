package ro.minipay.network.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ro.minipay.network.dto.AuthorizeResponse;

import java.util.Map;

/**
 * REST client for issuer-svc.
 *
 * Sends authorization requests to the issuer bank simulator.
 * The request contains the original PAN (detokenized by VaultClient).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IssuerClient {

    private final RestTemplate restTemplate;

    @Value("${services.issuer-url}")
    private String issuerUrl;

    /**
     * Send an authorization request to issuer-svc.
     * Returns system error response if issuer-svc is unavailable.
     */
    public AuthorizeResponse authorize(String pan, String expiryDate,
                                       Long amount, String currency,
                                       String merchantId, String txnId) {
        try {
            Map<String, Object> body = Map.of(
                "pan",        pan,
                "expiryDate", expiryDate != null ? expiryDate : "",
                "amount",     amount,
                "currency",   currency,
                "merchantId", merchantId,
                "txnId",      txnId
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                issuerUrl + "/issuer/authorize", body, Map.class
            );

            if (response == null) return AuthorizeResponse.systemError();

            String responseCode = (String) response.getOrDefault("responseCode", "96");
            String authCode     = (String) response.getOrDefault("authCode", "");
            log.info("Issuer response: responseCode={} authCode={} txnId={}",
                responseCode, authCode, txnId);
            return new AuthorizeResponse(responseCode, authCode);

        } catch (Exception e) {
            log.error("issuer-svc unavailable: {}", e.getMessage());
            return AuthorizeResponse.systemError();
        }
    }
}
