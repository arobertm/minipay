package ro.minipay.gateway.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * REST client for fraud-svc (Python FastAPI).
 *
 * Returns a fraud score (0.0 - 1.0) and SHAP explanation.
 * If fraud-svc is unavailable, defaults to score=0.0 (fail-open).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FraudClient {

    private final RestTemplate restTemplate;

    @Value("${services.fraud-url}")
    private String fraudUrl;

    public FraudResult score(String dpan, Long amount, String currency,
                              String merchantId, String ipAddress) {
        try {
            Map<String, Object> body = Map.of(
                "dpan",       dpan,
                "amount",     amount,
                "currency",   currency,
                "merchantId", merchantId,
                "ipAddress",  ipAddress != null ? ipAddress : ""
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                fraudUrl + "/fraud/score", new HttpEntity<>(body, headers), Map.class);

            if (response == null) return FraudResult.safe();

            double score   = ((Number) response.getOrDefault("score", 0.0)).doubleValue();
            List<String> reasons = castToStringList(response.get("reasons"));
            log.info("Fraud score: {} for DPAN {}****{}", score,
                dpan.substring(0, 6), dpan.substring(12));
            return new FraudResult(score, reasons);

        } catch (Exception e) {
            // Fail-open: if fraud-svc is down, allow transaction but log warning
            log.warn("fraud-svc unavailable, defaulting to score=0.0: {}", e.getMessage());
            return FraudResult.safe();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> castToStringList(Object obj) {
        if (obj instanceof List<?> list) {
            return (List<String>) list;
        }
        return Collections.emptyList();
    }

    public record FraudResult(double score, List<String> reasons) {
        public static FraudResult safe() {
            return new FraudResult(0.0, Collections.emptyList());
        }
        public boolean isBlocked()   { return score >= 0.8; }
        public boolean needsChallenge() { return score >= 0.5 && score < 0.8; }
    }
}
