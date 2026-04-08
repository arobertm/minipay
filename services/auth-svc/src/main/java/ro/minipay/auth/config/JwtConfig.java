package ro.minipay.auth.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * JWT configuration using RSA-256.
 *
 * NOTE: Currently using RS256 (RSA-SHA256) for JWT signing.
 * Plan to upgrade to CRYSTALS-Dilithium3 (NIST FIPS 204 post-quantum)
 * once Bouncycastle 1.79+ provides mature Dilithium3 support.
 *
 * For now, RS256 is sufficient for OAuth2/OIDC and maintains compatibility
 * with standard JWT clients and verification libraries.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class JwtConfig {

    /**
     * Generate RSA keypair for JWT signing.
     */
    @Bean
    public KeyPair keyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        log.info("Generated RSA-2048 keypair for JWT signing");
        return keyPair;
    }

    /**
     * Create JWT encoder (signer).
     */
    @Bean
    public JwtEncoder jwtEncoder(KeyPair keyPair) throws JOSEException {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.randomUUID().toString())
            .build();

        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(jwkSource);
    }

    /**
     * Create JWT decoder (verifier).
     */
    @Bean
    public JwtDecoder jwtDecoder(KeyPair keyPair) throws JOSEException {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
            .keyID(UUID.randomUUID().toString())
            .build();

        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }
}

