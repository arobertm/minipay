package ro.minipay.auth.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Repository;
import ro.minipay.auth.shared.minids.MiniDSClientService;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Authorization Server RegisteredClientRepository backed by MiniDS.
 *
 * Implements OAuth2 client registration storage (clients are stored in MiniDS).
 * Converts between Spring's RegisteredClient and MiniDS Entry format.
 *
 * DN Pattern: cn=<client_id>,ou=clients,dc=minipay,dc=ro
 * ObjectClass: oauth2Client
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MiniDSRegisteredClientRepository implements RegisteredClientRepository {

    private final MiniDSClientService minidsClient;

    private static final String CLIENTS_BASE_DN = "ou=clients,dc=minipay,dc=ro";

    /**
     * Save or update a registered client.
     */
    @Override
    public void save(RegisteredClient registeredClient) {
        if (registeredClient == null || registeredClient.getClientId() == null) {
            throw new IllegalArgumentException("RegisteredClient and clientId must not be null");
        }

        String clientId = registeredClient.getClientId();
        String dn = buildClientDn(clientId);

        log.debug("Saving OAuth2 client: {}", clientId);

        MiniDSClientService.Entry entry = new MiniDSClientService.Entry(
            dn,
            "oauth2Client",
            buildClientAttributes(registeredClient)
        );

        try {
            minidsClient.getEntry(dn).ifPresentOrElse(
                existing -> minidsClient.putEntry(dn, entry),
                () -> minidsClient.createEntry(entry)
            );
        } catch (Exception e) {
            log.error("Failed to save client: {}", clientId, e);
            throw new RuntimeException("Failed to save client", e);
        }
    }

    /**
     * Find a client by Spring's internal ID.
     */
    @Override
    public RegisteredClient findById(String id) {
        if (id == null) return null;

        // For now, assume id is clientId
        // In a real implementation, we might need to search by internal ID
        return findByClientId(id);
    }

    /**
     * Find a client by clientId.
     */
    @Override
    public RegisteredClient findByClientId(String clientId) {
        if (clientId == null) return null;

        String dn = buildClientDn(clientId);
        log.debug("Finding OAuth2 client by ID: {}", clientId);

        try {
            return minidsClient.getEntry(dn)
                .map(this::toRegisteredClient)
                .orElse(null);
        } catch (Exception e) {
            log.error("Failed to find client: {}", clientId, e);
            return null;
        }
    }

    /**
     * Search for clients by a filter attribute (e.g., by merchantId).
     */
    public java.util.List<RegisteredClient> findByAttribute(String attrName, String attrValue) {
        log.debug("Searching clients by {}: {}", attrName, attrValue);

        try {
            return minidsClient.search(CLIENTS_BASE_DN, Map.of(attrName, attrValue), 100)
                .stream()
                .map(this::toRegisteredClient)
                .toList();
        } catch (Exception e) {
            log.error("Failed to search clients", e);
            return java.util.Collections.emptyList();
        }
    }

    /* ─── Private Helpers ─── */

    private String buildClientDn(String clientId) {
        return "cn=" + clientId + "," + CLIENTS_BASE_DN;
    }

    private Map<String, String> buildClientAttributes(RegisteredClient client) {
        Map<String, String> attrs = new HashMap<>();

        attrs.put("clientId", client.getClientId());
        attrs.put("clientSecret", client.getClientSecret() != null ? client.getClientSecret() : "");
        attrs.put("clientName", client.getClientName() != null ? client.getClientName() : "");

        // Redirect URIs (comma-separated)
        if (!client.getRedirectUris().isEmpty()) {
            attrs.put("redirectUris", String.join(",", client.getRedirectUris()));
        }

        // Grant types (comma-separated)
        if (!client.getAuthorizationGrantTypes().isEmpty()) {
            attrs.put("grantTypes", client.getAuthorizationGrantTypes().stream()
                .map(g -> g.getValue())
                .reduce("", (a, b) -> String.join(",", a, b)));
        }

        // Scopes (comma-separated)
        if (!client.getScopes().isEmpty()) {
            attrs.put("scopes", String.join(",", client.getScopes()));
        }

        // Token TTLs
        if (client.getTokenSettings() != null) {
            attrs.put("accessTokenTtl", String.valueOf(
                client.getTokenSettings().getAccessTokenTimeToLive().getSeconds()));
            attrs.put("refreshTokenTtl", String.valueOf(
                client.getTokenSettings().getRefreshTokenTimeToLive().getSeconds()));
        }

        return attrs;
    }

    private RegisteredClient toRegisteredClient(MiniDSClientService.Entry entry) {
        Map<String, String> attrs = entry.getAttributes();
        if (attrs == null || !attrs.containsKey("clientId")) {
            return null;
        }

        return RegisteredClient.withId(attrs.get("clientId"))
            .clientId(attrs.get("clientId"))
            .clientSecret(attrs.get("clientSecret"))
            .clientName(attrs.getOrDefault("clientName", ""))
            .redirectUris(uris -> {
                String redirectUrisStr = attrs.get("redirectUris");
                if (redirectUrisStr != null) {
                    for (String uri : redirectUrisStr.split(",")) {
                        if (!uri.isEmpty()) uris.add(uri);
                    }
                }
            })
            .authorizationGrantTypes(types -> {
                String grantTypesStr = attrs.get("grantTypes");
                if (grantTypesStr != null) {
                    for (String type : grantTypesStr.split(",")) {
                        if (!type.isEmpty()) {
                            types.add(new org.springframework.security.oauth2.core.AuthorizationGrantType(type));
                        }
                    }
                }
            })
            .scopes(scopes -> {
                String scopesStr = attrs.get("scopes");
                if (scopesStr != null) {
                    for (String scope : scopesStr.split(",")) {
                        if (!scope.isEmpty()) scopes.add(scope);
                    }
                }
            })
            .postLogoutRedirectUris(uris -> {
                String redirectUrisStr = attrs.get("redirectUris");
                if (redirectUrisStr != null) {
                    for (String uri : redirectUrisStr.split(",")) {
                        if (!uri.isEmpty()) uris.add(uri);
                    }
                }
            })
            .tokenSettings(tokenSettings -> {
                String accessTtl = attrs.get("accessTokenTtl");
                if (accessTtl != null) {
                    tokenSettings.accessTokenTimeToLive(
                        java.time.Duration.ofSeconds(Long.parseLong(accessTtl))
                    );
                }
                String refreshTtl = attrs.get("refreshTokenTtl");
                if (refreshTtl != null) {
                    tokenSettings.refreshTokenTimeToLive(
                        java.time.Duration.ofSeconds(Long.parseLong(refreshTtl))
                    );
                }
            })
            .build();
    }
}
