package ro.minipay.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHAKEDigest;
import org.bouncycastle.crypto.signers.Dilithium;
import org.bouncycastle.crypto.signers.DilithiumSigner;
import org.bouncycastle.pqc.crypto.crystals.DilithiumParameters;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import ro.minipay.auth.crypto.DilithiumKeyProvider;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Custom JWT Encoder/Decoder using CRYSTALS-Dilithium3.
 *
 * Spring Authorization Server supports RS256, ES256, etc. by default,
 * but not post-quantum algorithms. This implementation creates JWTs
 * with DILITHIUM3 digital signatures.
 *
 * JWT format: header.payload.signature (standard JWT, but signature is Dilithium3)
 * Header: {"alg":"DILITHIUM3","typ":"JWT"}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DilithiumJwtEncoder implements JwtEncoder {

    private final DilithiumKeyProvider keyProvider;
    private final ObjectMapper objectMapper;

    /**
     * Encode a JWT with Dilithium3 signature.
     */
    @Override
    public Jwt encode(JwtEncoderParameters parameters) throws JwtEncodingException {
        try {
            JwsHeader headers = parameters.getJwsHeader();
            JwtClaimsSet claims = parameters.getClaims();

            // Build header
            Map<String, Object> headerMap = new HashMap<>();
            headerMap.put("alg", "DILITHIUM3");
            headerMap.put("typ", "JWT");

            // Build claims
            Map<String, Object> claimsMap = new HashMap<>();
            claimsMap.put("sub", claims.getSubject());
            claimsMap.put("aud", claims.getAudience());
            claimsMap.put("scope", claims.getClaim("scope"));
            claimsMap.put("client_id", claims.getClaim("client_id"));
            claimsMap.put("iat", claims.getIssuedAt().getEpochSecond());
            claimsMap.put("exp", claims.getExpiresAt().getEpochSecond());
            claimsMap.put("jti", UUID.randomUUID().toString());

            // Serialize header and payload
            String headerJson = objectMapper.writeValueAsString(headerMap);
            String payloadJson = objectMapper.writeValueAsString(claimsMap);

            String headerEncoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadEncoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            // Create signing input
            String signingInput = headerEncoded + "." + payloadEncoded;
            byte[] signingBytes = signingInput.getBytes(StandardCharsets.UTF_8);

            // Sign with Dilithium3
            DilithiumSigner signer = new DilithiumSigner();
            signer.init(true, keyProvider.getPrivateKey());
            byte[] signature = signer.generateSignature(signingBytes);

            // Encode signature
            String signatureEncoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(signature);

            // Assemble JWT
            String jwtToken = signingInput + "." + signatureEncoded;

            log.debug("Issued JWT with Dilithium3 signature");

            return new Jwt(jwtToken, claims.getIssuedAt(), claims.getExpiresAt(),
                new java.util.HashMap<>(Map.of("alg", "DILITHIUM3")),
                claimsMap);
        } catch (Exception e) {
            log.error("Failed to encode JWT", e);
            throw new JwtEncodingException("Dilithium3 JWT encoding failed", e);
        }
    }
}

/**
 * Custom JWT Decoder using CRYSTALS-Dilithium3.
 * Verifies JWT signatures created with DilithiumJwtEncoder.
 */
@Slf4j
@Component
class DilithiumJwtDecoder implements JwtDecoder {

    private final DilithiumKeyProvider keyProvider;
    private final ObjectMapper objectMapper;

    public DilithiumJwtDecoder(DilithiumKeyProvider keyProvider, ObjectMapper objectMapper) {
        this.keyProvider = keyProvider;
        this.objectMapper = objectMapper;
    }

    /**
     * Decode and verify a JWT with Dilithium3 signature.
     */
    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            // Parse JWT format: header.payload.signature
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new JwtException("Invalid JWT format");
            }

            String headerEncoded = parts[0];
            String payloadEncoded = parts[1];
            String signatureEncoded = parts[2];

            // Decode header and payload
            byte[] headerBytes = Base64.getUrlDecoder().decode(addPadding(headerEncoded));
            byte[] payloadBytes = Base64.getUrlDecoder().decode(addPadding(payloadEncoded));
            byte[] signatureBytes = Base64.getUrlDecoder().decode(addPadding(signatureEncoded));

            // Parse claims
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(payloadBytes, Map.class);

            // Verify signature
            String signingInput = headerEncoded + "." + payloadEncoded;
            byte[] signingBytes = signingInput.getBytes(StandardCharsets.UTF_8);

            DilithiumSigner verifier = new DilithiumSigner();
            verifier.init(false, keyProvider.getPublicKey());
            boolean isValid = verifier.verifySignature(signingBytes, signatureBytes);

            if (!isValid) {
                throw new JwtException("Invalid Dilithium3 signature");
            }

            // Validate expiration
            if (claims.containsKey("exp")) {
                long expirationTime = ((Number) claims.get("exp")).longValue();
                if (Instant.now().getEpochSecond() > expirationTime) {
                    throw new JwtException("JWT has expired");
                }
            }

            log.debug("JWT verified with Dilithium3 signature");

            Instant issuedAt = claims.containsKey("iat")
                ? Instant.ofEpochSecond(((Number) claims.get("iat")).longValue())
                : Instant.now();
            Instant expiresAt = claims.containsKey("exp")
                ? Instant.ofEpochSecond(((Number) claims.get("exp")).longValue())
                : Instant.now().plusSeconds(3600);

            return new Jwt(token, issuedAt, expiresAt,
                Map.of("alg", "DILITHIUM3"),
                claims);
        } catch (JwtException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to decode JWT", e);
            throw new JwtException("Dilithium3 JWT decoding failed", e);
        }
    }

    /**
     * Add padding to Base64URL string for decoding.
     */
    private String addPadding(String encodedString) {
        switch (encodedString.length() % 4) {
            case 2:
                return encodedString + "==";
            case 3:
                return encodedString + "=";
            default:
                return encodedString;
        }
    }
}
