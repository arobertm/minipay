package ro.minipay.auth.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumPublicKeyParameters;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumSigner;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Post-quantum JWT signer using CRYSTALS-Dilithium3 (NIST FIPS 204).
 *
 * Produces compact JWS tokens in standard format: header.payload.signature
 *
 * Algorithm properties:
 *  - Security level: NIST Level 3 (equivalent to AES-192)
 *  - Quantum security: ~256 bits
 *  - Public key size:  1952 bytes
 *  - Signature size:   3293 bytes  (vs 256 bytes for RSA-2048)
 *  - Based on: Module Learning With Errors (MLWE) hard problem
 *
 * Note: DILITHIUM3 is not a registered IANA algorithm identifier yet.
 * This is a research/dissertation implementation demonstrating PQC readiness.
 * RFC status: https://datatracker.ietf.org/doc/draft-ietf-cose-dilithium/
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DilithiumJwtSigner {

    private static final Base64.Encoder BASE64URL = Base64.getUrlEncoder().withoutPadding();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DilithiumPrivateKeyParameters dilithiumPrivateKey;
    private final DilithiumPublicKeyParameters dilithiumPublicKey;

    /**
     * Sign a JWT with Dilithium3.
     *
     * @param subject   token subject (user/service ID)
     * @param audience  intended audience (client ID)
     * @param scopes    space-separated OAuth2 scopes
     * @param ttlSeconds token lifetime in seconds
     * @return compact JWS: base64url(header).base64url(payload).base64url(signature)
     */
    public String sign(String subject, String audience, String scopes, long ttlSeconds) {
        try {
            Instant now = Instant.now();

            // 1. Build JWT header
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "DILITHIUM3");
            header.put("typ", "JWT");
            header.put("kid", "dil3-1");

            // 2. Build JWT claims
            Map<String, Object> claims = new LinkedHashMap<>();
            claims.put("iss", "http://auth-svc:8081");
            claims.put("sub", subject);
            claims.put("aud", audience);
            claims.put("iat", now.getEpochSecond());
            claims.put("exp", now.plusSeconds(ttlSeconds).getEpochSecond());
            claims.put("scope", scopes);
            claims.put("pqc", true);                    // PQC marker claim
            claims.put("alg_family", "CRYSTALS");       // algorithm family
            claims.put("nist_level", 3);                // NIST security level

            // 3. Encode header + payload as Base64url
            String encodedHeader  = BASE64URL.encodeToString(MAPPER.writeValueAsBytes(header));
            String encodedPayload = BASE64URL.encodeToString(MAPPER.writeValueAsBytes(claims));
            String signingInput   = encodedHeader + "." + encodedPayload;

            // 4. Sign with Dilithium3
            byte[] message = signingInput.getBytes(StandardCharsets.US_ASCII);
            DilithiumSigner signer = new DilithiumSigner();
            signer.init(true, dilithiumPrivateKey);
            byte[] signature = signer.generateSignature(message);

            String encodedSig = BASE64URL.encodeToString(signature);
            log.debug("Signed PQC JWT for sub={} with Dilithium3 ({} byte signature)",
                subject, signature.length);

            return signingInput + "." + encodedSig;

        } catch (Exception e) {
            throw new RuntimeException("Failed to sign Dilithium3 JWT", e);
        }
    }

    /**
     * Verify a Dilithium3-signed JWT (compact form).
     *
     * @param token compact JWS token
     * @return true if signature is valid
     */
    public boolean verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;

            String signingInput = parts[0] + "." + parts[1];
            byte[] message   = signingInput.getBytes(StandardCharsets.US_ASCII);
            byte[] signature = Base64.getUrlDecoder().decode(parts[2]);

            DilithiumSigner verifier = new DilithiumSigner();
            verifier.init(false, dilithiumPublicKey);
            return verifier.verifySignature(message, signature);

        } catch (Exception e) {
            log.warn("Dilithium3 JWT verification failed: {}", e.getMessage());
            return false;
        }
    }
}
