package ro.minipay.auth.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentRepository;
import org.springframework.stereotype.Repository;
import ro.minipay.auth.shared.minids.MiniDSClientService;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Authorization Server OAuth2AuthorizationConsentRepository backed by MiniDS.
 *
 * Stores user consent records (user approved scopes for a specific client).
 * Used in the authorization_code flow to track user consent decisions.
 *
 * DN Pattern: cn=<client_id>+<principal_name>,ou=consents,dc=minipay,dc=ro
 * ObjectClass: oauth2Consent
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MiniDSAuthorizationConsentRepository implements OAuth2AuthorizationConsentRepository {

    private final MiniDSClientService minidsClient;

    private static final String CONSENTS_BASE_DN = "ou=consents,dc=minipay,dc=ro";

    /**
     * Save a user's consent for a client.
     */
    @Override
    public void save(OAuth2AuthorizationConsent authorizationConsent) {
        if (authorizationConsent == null
            || authorizationConsent.getRegisteredClientId() == null
            || authorizationConsent.getPrincipalName() == null) {
            throw new IllegalArgumentException("OAuth2AuthorizationConsent data must not be null");
        }

        String clientId = authorizationConsent.getRegisteredClientId();
        String principalName = authorizationConsent.getPrincipalName();
        String dn = buildConsentDn(clientId, principalName);

        log.debug("Saving authorization consent: client={}, principal={}", clientId, principalName);

        MiniDSClientService.Entry entry = new MiniDSClientService.Entry(
            dn,
            "oauth2Consent",
            buildConsentAttributes(authorizationConsent)
        );

        try {
            minidsClient.getEntry(dn).ifPresentOrElse(
                existing -> minidsClient.putEntry(dn, entry),
                () -> minidsClient.createEntry(entry)
            );
        } catch (Exception e) {
            log.error("Failed to save consent: {}, {}", clientId, principalName, e);
            throw new RuntimeException("Failed to save consent", e);
        }
    }

    /**
     * Delete a user's consent for a client.
     */
    @Override
    public void remove(OAuth2AuthorizationConsent authorizationConsent) {
        if (authorizationConsent == null
            || authorizationConsent.getRegisteredClientId() == null
            || authorizationConsent.getPrincipalName() == null) {
            return;
        }

        String clientId = authorizationConsent.getRegisteredClientId();
        String principalName = authorizationConsent.getPrincipalName();
        String dn = buildConsentDn(clientId, principalName);

        log.debug("Removing authorization consent: client={}, principal={}", clientId, principalName);

        try {
            minidsClient.deleteEntry(dn);
        } catch (Exception e) {
            log.error("Failed to remove consent: {}, {}", clientId, principalName, e);
        }
    }

    /**
     * Find a user's consent for a specific client.
     */
    @Override
    public OAuth2AuthorizationConsent findById(String registeredClientId, String principalName) {
        if (registeredClientId == null || principalName == null) {
            return null;
        }

        String dn = buildConsentDn(registeredClientId, principalName);
        log.debug("Finding authorization consent: client={}, principal={}", registeredClientId, principalName);

        try {
            return minidsClient.getEntry(dn)
                .map(this::toOAuth2AuthorizationConsent)
                .orElse(null);
        } catch (Exception e) {
            log.error("Failed to find consent: {}, {}", registeredClientId, principalName, e);
            return null;
        }
    }

    /* ─── Private Helpers ─── */

    private String buildConsentDn(String clientId, String principalName) {
        // Use a combined key in the CN
        String cnValue = clientId + "+" + principalName;
        return "cn=" + cnValue + "," + CONSENTS_BASE_DN;
    }

    private Map<String, String> buildConsentAttributes(OAuth2AuthorizationConsent consent) {
        Map<String, String> attrs = new HashMap<>();

        attrs.put("clientId", consent.getRegisteredClientId());
        attrs.put("principalName", consent.getPrincipalName());

        // Scopes (comma-separated)
        if (!consent.getAuthorities().isEmpty()) {
            String scopes = consent.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replace("SCOPE_", ""))
                .reduce("", (a, b) -> a.isEmpty() ? b : String.join(",", a, b));
            attrs.put("scopes", scopes);
        }

        // Metadata
        attrs.put("createdAt", java.time.Instant.now().toString());

        return attrs;
    }

    private OAuth2AuthorizationConsent toOAuth2AuthorizationConsent(MiniDSClientService.Entry entry) {
        Map<String, String> attrs = entry.getAttributes();
        if (attrs == null || !attrs.containsKey("clientId") || !attrs.containsKey("principalName")) {
            return null;
        }

        String clientId = attrs.get("clientId");
        String principalName = attrs.get("principalName");

        OAuth2AuthorizationConsent.Builder builder = OAuth2AuthorizationConsent
            .withId(clientId, principalName);

        // Add scopes as authorities
        String scopesStr = attrs.get("scopes");
        if (scopesStr != null && !scopesStr.isEmpty()) {
            for (String scope : scopesStr.split(",")) {
                if (!scope.isEmpty()) {
                    builder.authority(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_" + scope));
                }
            }
        }

        return builder.build();
    }
}
