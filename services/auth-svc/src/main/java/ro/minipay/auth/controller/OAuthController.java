package ro.minipay.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumPublicKeyParameters;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.minipay.auth.crypto.DilithiumJwtSigner;

import java.security.interfaces.RSAPublicKey;
import java.util.*;

/**
 * OAuth2/OIDC endpoints — standard Spring Authorization Server + PQC extensions.
 *
 * Standard endpoints (handled by Spring):
 *   POST /oauth2/token          — issue RS256 access token
 *   POST /oauth2/introspect     — token introspection
 *   POST /oauth2/revoke         — token revocation
 *
 * Custom endpoints (this controller):
 *   GET  /oauth2/jwks           — JWKS with RSA + Dilithium3 public keys
 *   GET  /.well-known/...       — OIDC discovery metadata (Spring auto + override)
 *   POST /auth/token/pqc        — issue Dilithium3-signed PQC JWT
 *   POST /auth/token/pqc/verify — verify a Dilithium3 JWT
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class OAuthController {

    private final RSAPublicKey rsaPublicKey;
    private final DilithiumPublicKeyParameters dilithiumPublicKey;
    private final DilithiumJwtSigner dilithiumJwtSigner;

    // ─── JWKS ─────────────────────────────────────────────────────────────────

    /**
     * GET /oauth2/jwks
     *
     * Returns both RSA-2048 (RS256) and Dilithium3 public keys in JWKS format.
     *
     * Dilithium3 key uses OKP key type (RFC 8037 pattern) with:
     *   "crv": "Dilithium3"  — non-standard, follows IETF draft-ietf-cose-dilithium
     *   "x":   base64url(publicKeyBytes)
     */
    @GetMapping(path = "/oauth2/jwks", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getJwks() {
        List<Map<String, Object>> keys = new ArrayList<>();

        // RSA-2048 key (RS256) — standard OAuth2/OIDC
        Map<String, Object> rsaKey = new LinkedHashMap<>();
        rsaKey.put("kty", "RSA");
        rsaKey.put("alg", "RS256");
        rsaKey.put("use", "sig");
        rsaKey.put("kid", "rsa-2048-1");
        rsaKey.put("n", Base64.getUrlEncoder().withoutPadding()
            .encodeToString(rsaPublicKey.getModulus().toByteArray()));
        rsaKey.put("e", "AQAB");
        keys.add(rsaKey);

        // Dilithium3 key (CRYSTALS, NIST FIPS 204) — post-quantum
        byte[] dilPubBytes = dilithiumPublicKey.getEncoded();
        Map<String, Object> dilKey = new LinkedHashMap<>();
        dilKey.put("kty", "OKP");
        dilKey.put("alg", "DILITHIUM3");
        dilKey.put("use", "sig");
        dilKey.put("kid", "dil3-1");
        dilKey.put("crv", "Dilithium3");
        dilKey.put("x", Base64.getUrlEncoder().withoutPadding().encodeToString(dilPubBytes));
        dilKey.put("key_size", dilPubBytes.length);           // 1952 bytes
        dilKey.put("nist_level", 3);
        keys.add(dilKey);

        log.debug("JWKS requested — returning {} keys (RS256 + DILITHIUM3)", keys.size());
        return ResponseEntity.ok(Map.of("keys", keys));
    }

    // ─── PQC Token endpoints ──────────────────────────────────────────────────

    /**
     * POST /auth/token/pqc
     *
     * Issues a Dilithium3-signed JWT.
     * This is a research endpoint demonstrating post-quantum JWT signing.
     *
     * Request params (form or JSON):
     *   subject   — token subject (default: "demo-client")
     *   audience  — intended audience (default: "minipay")
     *   scope     — space-separated scopes (default: "payments:read")
     *   ttl       — lifetime in seconds (default: 3600)
     */
    @PostMapping(path = "/auth/token/pqc", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> issuePqcToken(
            @RequestParam(defaultValue = "demo-client") String subject,
            @RequestParam(defaultValue = "minipay")     String audience,
            @RequestParam(defaultValue = "payments:read") String scope,
            @RequestParam(defaultValue = "3600")        long ttl) {

        String token = dilithiumJwtSigner.sign(subject, audience, scope, ttl);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("access_token", token);
        response.put("token_type", "Bearer");
        response.put("expires_in", ttl);
        response.put("algorithm", "DILITHIUM3");
        response.put("nist_level", 3);
        response.put("scope", scope);

        log.info("Issued Dilithium3 PQC JWT for sub={}", subject);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /auth/token/pqc/verify
     *
     * Verifies a Dilithium3-signed JWT and returns its decoded claims.
     */
    @PostMapping(path = "/auth/token/pqc/verify", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> verifyPqcToken(@RequestParam String token) {
        boolean valid = dilithiumJwtSigner.verify(token);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("valid", valid);
        response.put("algorithm", "DILITHIUM3");

        if (valid) {
            // Decode claims (no verification needed here — already verified above)
            try {
                String[] parts = token.split("\\.");
                byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
                String payloadJson = new String(payloadBytes);
                response.put("active", true);
                response.put("claims_raw", payloadJson);
            } catch (Exception e) {
                response.put("decode_error", e.getMessage());
            }
        } else {
            response.put("active", false);
            response.put("error", "invalid_token");
        }

        return ResponseEntity.ok(response);
    }

    // ─── OIDC Discovery override ──────────────────────────────────────────────

    /**
     * GET /oauth2/server-metadata-pqc
     *
     * Extended OIDC discovery including PQC capabilities.
     * Spring's built-in /.well-known endpoint doesn't know about Dilithium3.
     */
    @GetMapping(path = "/oauth2/server-metadata-pqc", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getPqcServerMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();

        metadata.put("issuer", "http://auth-svc:8081");
        metadata.put("authorization_endpoint", "http://auth-svc:8081/oauth2/authorize");
        metadata.put("token_endpoint", "http://auth-svc:8081/oauth2/token");
        metadata.put("pqc_token_endpoint", "http://auth-svc:8081/auth/token/pqc");
        metadata.put("jwks_uri", "http://auth-svc:8081/oauth2/jwks");
        metadata.put("introspection_endpoint", "http://auth-svc:8081/oauth2/introspect");
        metadata.put("revocation_endpoint", "http://auth-svc:8081/oauth2/revoke");

        // Both classical and post-quantum algorithms
        metadata.put("id_token_signing_alg_values_supported", Arrays.asList("RS256", "DILITHIUM3"));
        metadata.put("token_endpoint_auth_methods_supported",
            Arrays.asList("client_secret_basic", "client_secret_post"));

        metadata.put("grant_types_supported",
            Arrays.asList("authorization_code", "client_credentials", "refresh_token"));

        metadata.put("scopes_supported",
            Arrays.asList("openid", "profile", "email", "payments:read", "payments:write", "tokens:revoke"));

        // PQC capabilities
        Map<String, Object> pqc = new LinkedHashMap<>();
        pqc.put("algorithms_supported", List.of("DILITHIUM3"));
        pqc.put("nist_fips", "204");
        pqc.put("security_level", 3);
        pqc.put("public_key_size_bytes", 1952);
        pqc.put("signature_size_bytes", 3293);
        metadata.put("pqc_capabilities", pqc);

        return ResponseEntity.ok(metadata);
    }
}
