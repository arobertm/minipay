package ro.minipay.network.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * REST client for vault-svc detokenization.
 *
 * network-svc calls vault-svc to resolve DPAN → PAN before forwarding
 * the authorization request to issuer-svc.
 *
 * This is the standard EMV network detokenization flow:
 *   gateway (DPAN) → network (detokenize) → issuer (PAN)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VaultClient {

    private final RestTemplate restTemplate;

    @Value("${services.vault-url}")
    private String vaultUrl;

    public record DetokenizeResult(String pan, String expiryDate) {}

    /**
     * Detokenize a DPAN to get the original PAN.
     * Returns null if vault-svc is unavailable or token not found.
     */
    public DetokenizeResult detokenize(String dpan) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> response = restTemplate.postForObject(
                vaultUrl + "/vault/detokenize/" + dpan,
                null,
                Map.class
            );
            if (response == null) return null;

            log.debug("DPAN detokenized: {}****", dpan.substring(0, Math.min(6, dpan.length())));
            return new DetokenizeResult(
                response.get("pan"),
                response.getOrDefault("expiryDate", "")
            );
        } catch (Exception e) {
            log.error("vault-svc detokenize failed for dpan={}****: {}",
                dpan.substring(0, Math.min(6, dpan.length())), e.getMessage());
            return null;
        }
    }
}
