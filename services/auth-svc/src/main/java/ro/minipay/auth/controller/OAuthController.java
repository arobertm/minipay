package ro.minipay.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * REST controller for OAuth2/OIDC endpoints.
 *
 * Provides:
 * - /oauth2/jwks — JSON Web Key Set (public key for JWT verification)
 * - /.well-known/oauth-authorization-server — OIDC server metadata discovery
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class OAuthController {

    private final JwtEncoder jwtEncoder;

    /**
     * GET /oauth2/jwks
     *
     * Returns the public key in JWKS format for JWT verification.
     * Clients use this to verify JWTs signed by this server.
     *
     * JWKS spec: https://tools.ietf.org/html/rfc7517
     *
     * NOTE: Currently using RS256. Will upgrade to Dilithium3 (PQC) in future versions.
     */
    @GetMapping(path = "/oauth2/jwks", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getJwks() {
        try {
            // Build minimal JWKS response
            // In production, extract actual public key from jwtEncoder
            Map<String, Object> key = new LinkedHashMap<>();
            key.put("kty", "RSA");
            key.put("alg", "RS256");
            key.put("use", "sig");
            key.put("kid", "rsa-1");

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("keys", List.of(key));

            log.debug("JWKS endpoint called");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating JWKS", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * GET /.well-known/oauth-authorization-server
     *
     * Returns OAuth2 server metadata for OIDC discovery.
     * Clients use this to discover endpoints and capabilities.
     *
     * Spec: https://tools.ietf.org/html/draft-ietf-oauth-discovery-10
     */
    @GetMapping(path = "/.well-known/oauth-authorization-server", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getAuthorizationServerMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();

        // Server information
        metadata.put("issuer", "http://auth-svc:8081");
        metadata.put("authorization_endpoint", "http://auth-svc:8081/oauth2/authorize");
        metadata.put("token_endpoint", "http://auth-svc:8081/oauth2/token");
        metadata.put("token_introspection_endpoint", "http://auth-svc:8081/oauth2/introspect");
        metadata.put("token_revocation_endpoint", "http://auth-svc:8081/oauth2/revoke");
        metadata.put("jwks_uri", "http://auth-svc:8081/oauth2/jwks");
        metadata.put("userinfo_endpoint", "http://auth-svc:8081/oauth2/userinfo");
        metadata.put("logout_endpoint", "http://auth-svc:8081/oauth2/logout");

        // Supported grant types
        metadata.put("grant_types_supported", Arrays.asList(
            "authorization_code",
            "client_credentials",
            "refresh_token"
        ));

        // Supported response types
        metadata.put("response_types_supported", Arrays.asList(
            "code",
            "token"
        ));

        // Supported token endpoint auth methods
        metadata.put("token_endpoint_auth_methods_supported", Arrays.asList(
            "client_secret_basic",
            "client_secret_post"
        ));

        // Supported algorithms (currently RS256, will add Dilithium3)
        metadata.put("id_token_signing_alg_values_supported", Arrays.asList(
            "RS256"
        ));

        // Supported scopes
        metadata.put("scopes_supported", Arrays.asList(
            "openid",
            "profile",
            "email",
            "payments:read",
            "payments:write",
            "tokens:revoke"
        ));

        // Claims supported
        metadata.put("claims_supported", Arrays.asList(
            "sub",
            "iss",
            "aud",
            "exp",
            "iat",
            "scope",
            "client_id"
        ));

        log.debug("OAuth2 server metadata requested");
        return ResponseEntity.ok(metadata);
    }
}

