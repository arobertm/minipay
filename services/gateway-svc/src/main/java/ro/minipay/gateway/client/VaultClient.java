package ro.minipay.gateway.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * REST client for vault-svc.
 * Handles PAN → DPAN tokenization.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VaultClient {

    private final RestTemplate restTemplate;

    @Value("${services.vault-url}")
    private String vaultUrl;

    /**
     * Tokenize a PAN — returns DPAN.
     * PAN is sent to vault-svc and never stored in gateway.
     */
    public String tokenize(String pan, String expiryDate) {
        Map<String, String> body = Map.of(
            "pan", pan,
            "expiryDate", expiryDate != null ? expiryDate : ""
        );
        Map<?, ?> response = restTemplate.postForObject(
            vaultUrl + "/vault/tokenize", body, Map.class);

        if (response == null || !response.containsKey("dpan")) {
            throw new RuntimeException("vault-svc tokenization failed");
        }
        String dpan = (String) response.get("dpan");
        log.debug("PAN tokenized → DPAN: {}****{}", dpan.substring(0, 6), dpan.substring(12));
        return dpan;
    }
}
