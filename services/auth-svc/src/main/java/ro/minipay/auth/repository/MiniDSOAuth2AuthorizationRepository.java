package ro.minipay.auth.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationRepository;
import org.springframework.stereotype.Repository;
import ro.minipay.auth.shared.minids.MiniDSClientService;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Spring Authorization Server OAuth2AuthorizationRepository backed by MiniDS + local cache.
 *
 * Stores OAuth2 authorizations (access tokens, refresh tokens, authorization codes).
 * Uses local Caffeine cache for frequent lookups (tokens are short-lived, so cache prevents
 * excessive MiniDS queries during token validation).
 *
 * DN Pattern: coreTokenId=<jti>,ou=tokens,dc=minipay,dc=ro
 * ObjectClass: coreToken
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MiniDSOAuth2AuthorizationRepository implements OAuth2AuthorizationRepository {

    private final MiniDSClientService minidsClient;

    private static final String TOKENS_BASE_DN = "ou=tokens,dc=minipay,dc=ro";

    // Local cache: tokens are short-lived (60 min access tokens, 30 day refresh tokens)
    // Cache for 5 minutes to reduce MiniDS queries during validation
    private final Cache<String, OAuth2Authorization> authorizationCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .recordStats()
        .build();

    /**
     * Save an OAuth2 authorization.
     */
    @Override
    public void save(OAuth2Authorization authorization) {
        if (authorization == null || authorization.getId() == null) {
            throw new IllegalArgumentException("OAuth2Authorization and id must not be null");
        }

        String jti = authorization.getId();
        String dn = buildTokenDn(jti);

        log.debug("Saving OAuth2 authorization: {}", jti);

        MiniDSClientService.Entry entry = new MiniDSClientService.Entry(
            dn,
            "coreToken",
            buildTokenAttributes(authorization)
        );

        try {
            minidsClient.getEntry(dn).ifPresentOrElse(
                existing -> minidsClient.putEntry(dn, entry),
                () -> minidsClient.createEntry(entry)
            );
            // Also cache locally
            authorizationCache.put(jti, authorization);
        } catch (Exception e) {
            log.error("Failed to save authorization: {}", jti, e);
            throw new RuntimeException("Failed to save authorization", e);
        }
    }

    /**
     * Remove an OAuth2 authorization by ID (used for revocation).
     */
    @Override
    public void remove(OAuth2Authorization authorization) {
        if (authorization == null || authorization.getId() == null) {
            return;
        }

        String jti = authorization.getId();
        String dn = buildTokenDn(jti);

        log.debug("Removing OAuth2 authorization: {}", jti);

        try {
            minidsClient.deleteEntry(dn);
            authorizationCache.invalidate(jti);
        } catch (Exception e) {
            log.error("Failed to remove authorization: {}", jti, e);
        }
    }

    /**
     * Find an authorization by ID.
     */
    @Override
    public OAuth2Authorization findById(String id) {
        if (id == null) return null;

        // Check cache first
        OAuth2Authorization cached = authorizationCache.getIfPresent(id);
        if (cached != null) {
            return cached;
        }

        String dn = buildTokenDn(id);
        log.debug("Finding authorization by ID: {}", id);

        try {
            OAuth2Authorization auth = minidsClient.getEntry(dn)
                .map(this::toOAuth2Authorization)
                .orElse(null);

            if (auth != null) {
                authorizationCache.put(id, auth);
            }
            return auth;
        } catch (Exception e) {
            log.error("Failed to find authorization: {}", id, e);
            return null;
        }
    }

    /**
     * Find an authorization by token value.
     * Used for token validation / introspection.
     */
    @Override
    public OAuth2Authorization findByToken(String token, org.springframework.security.oauth2.core.OAuth2TokenType tokenType) {
        if (token == null || tokenType == null) return null;

        String attrName = getTokenTypeAttribute(tokenType);
        log.debug("Finding authorization by token (type={})", tokenType);

        try {
            return minidsClient.search(TOKENS_BASE_DN, Map.of(attrName, token), 1)
                .stream()
                .findFirst()
                .map(this::toOAuth2Authorization)
                .orElse(null);
        } catch (Exception e) {
            log.error("Failed to find authorization by token", e);
            return null;
        }
    }

    /* ─── Private Helpers ─── */

    private String buildTokenDn(String jti) {
        return "coreTokenId=" + jti + "," + TOKENS_BASE_DN;
    }

    private Map<String, String> buildTokenAttributes(OAuth2Authorization auth) {
        Map<String, String> attrs = new HashMap<>();

        attrs.put("coreTokenId", auth.getId());
        attrs.put("coreTokenType", "OAUTH2_AUTHORIZATION");
        attrs.put("subject", auth.getPrincipalName());
        attrs.put("clientId", auth.getRegisteredClientId());

        // Access token
        OAuth2AccessToken accessToken = auth.getToken(OAuth2AccessToken.class);
        if (accessToken != null) {
            attrs.put("accessTokenValue", accessToken.getTokenValue());
            attrs.put("accessTokenIssuedAt", accessToken.getIssuedAt().toString());
            attrs.put("accessTokenExpiresAt", accessToken.getExpiresAt().toString());
            attrs.put("accessTokenScopes", String.join(",", accessToken.getScopes()));
        }

        // Refresh token
        OAuth2RefreshToken refreshToken = auth.getToken(OAuth2RefreshToken.class);
        if (refreshToken != null) {
            attrs.put("refreshTokenValue", refreshToken.getTokenValue());
            attrs.put("refreshTokenIssuedAt", refreshToken.getIssuedAt().toString());
            if (refreshToken.getExpiresAt() != null) {
                attrs.put("refreshTokenExpiresAt", refreshToken.getExpiresAt().toString());
            }
        }

        // Authorization code (if present)
        var authorizationCode = auth.getToken(org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest.class);
        if (authorizationCode != null && authorizationCode.getToken() != null) {
            attrs.put("authorizationCodeValue", authorizationCode.getToken().getTokenValue());
            attrs.put("authorizationCodeIssuedAt", authorizationCode.getToken().getIssuedAt().toString());
            attrs.put("authorizationCodeExpiresAt", authorizationCode.getToken().getExpiresAt().toString());
        }

        // Expiry for cleanup (use access token expiry as primary)
        if (accessToken != null && accessToken.getExpiresAt() != null) {
            attrs.put("coreTokenExpiry", accessToken.getExpiresAt().toString());
        }

        return attrs;
    }

    private OAuth2Authorization toOAuth2Authorization(MiniDSClientService.Entry entry) {
        Map<String, String> attrs = entry.getAttributes();
        if (attrs == null || !attrs.containsKey("coreTokenId")) {
            return null;
        }

        String jti = attrs.get("coreTokenId");
        String subject = attrs.get("subject");
        String clientId = attrs.get("clientId");

        // Reconstruct OAuth2Authorization
        org.springframework.security.oauth2.server.authorization.OAuth2Authorization.Builder builder =
            OAuth2Authorization.withId(jti)
                .principalName(subject)
                .registeredClientId(clientId);

        // Add tokens if present
        String accessTokenValue = attrs.get("accessTokenValue");
        if (accessTokenValue != null) {
            Instant issuedAt = Instant.parse(attrs.getOrDefault("accessTokenIssuedAt", Instant.now().toString()));
            Instant expiresAt = Instant.parse(attrs.getOrDefault("accessTokenExpiresAt", Instant.now().plusSeconds(3600).toString()));
            builder.token(new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                accessTokenValue,
                issuedAt,
                expiresAt,
                parseScopes(attrs.get("accessTokenScopes"))
            ));
        }

        String refreshTokenValue = attrs.get("refreshTokenValue");
        if (refreshTokenValue != null) {
            Instant issuedAt = Instant.parse(attrs.getOrDefault("refreshTokenIssuedAt", Instant.now().toString()));
            Instant expiresAt = attrs.containsKey("refreshTokenExpiresAt")
                ? Instant.parse(attrs.get("refreshTokenExpiresAt"))
                : null;
            builder.token(new OAuth2RefreshToken(refreshTokenValue, issuedAt, expiresAt));
        }

        return builder.build();
    }

    private String getTokenTypeAttribute(org.springframework.security.oauth2.core.OAuth2TokenType tokenType) {
        if (org.springframework.security.oauth2.core.OAuth2AccessToken.class.isAssignableFrom(tokenType.getClass())) {
            return "accessTokenValue";
        } else if (OAuth2RefreshToken.class.isAssignableFrom(tokenType.getClass())) {
            return "refreshTokenValue";
        } else {
            return "authorizationCodeValue";
        }
    }

    private java.util.Set<String> parseScopes(String scopesStr) {
        if (scopesStr == null || scopesStr.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        return java.util.Arrays.stream(scopesStr.split(","))
            .filter(s -> !s.isEmpty())
            .collect(java.util.stream.Collectors.toSet());
    }
}
