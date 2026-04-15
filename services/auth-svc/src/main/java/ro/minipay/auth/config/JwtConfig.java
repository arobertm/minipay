package ro.minipay.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumKeyPairGenerator;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumParameters;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumPublicKeyParameters;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * JWT configuration — RS256 (RSA-2048) + CRYSTALS-Dilithium3 (NIST FIPS 204).
 *
 * Two independent signing paths:
 *  - RS256  → standard OAuth2/OIDC, used by Spring Authorization Server
 *  - DILITHIUM3 → post-quantum, exposed via /auth/token/pqc and JWKS
 *
 * Dilithium3 uses the low-level Bouncy Castle API (BC 1.78.1+) directly,
 * because Nimbus JOSE does not support PQC algorithms yet.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class JwtConfig {

    @PostConstruct
    public void registerBouncyCastlePQC() {
        if (Security.getProvider("BCPQC") == null) {
            Security.addProvider(new BouncyCastlePQCProvider());
            log.info("Registered Bouncy Castle PQC provider (BCPQC)");
        }
    }

    // ─── RSA-2048 (RS256) ──────────────────────────────────────────────────────

    /**
     * RSA-2048 keypair — used by Spring Authorization Server for standard JWTs.
     */
    @Bean
    public KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        log.info("Generated RSA-2048 keypair for RS256 JWT signing");
        return kp;
    }

    @Bean
    public RSAPublicKey rsaPublicKey(KeyPair rsaKeyPair) {
        return (RSAPublicKey) rsaKeyPair.getPublic();
    }

    /**
     * JWKSource bean — used by Spring Authorization Server for BOTH token signing
     * AND the JWKS endpoint. Must be a bean so the JWKS endpoint returns the same key
     * that was used to sign the token.
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource(KeyPair rsaKeyPair) {
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) rsaKeyPair.getPublic())
            .privateKey((RSAPrivateKey) rsaKeyPair.getPrivate())
            .keyID(UUID.randomUUID().toString())
            .build();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(KeyPair rsaKeyPair) {
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) rsaKeyPair.getPublic()).build();
    }

    // ─── CRYSTALS-Dilithium3 (NIST FIPS 204) ──────────────────────────────────

    /**
     * Dilithium3 keypair using BC low-level API.
     *
     * NIST FIPS 204 security level 3 — 256-bit quantum security.
     * Public key: 1952 bytes | Private key: 4000 bytes | Signature: 3293 bytes
     */
    @Bean
    public AsymmetricCipherKeyPair dilithiumKeyPair() {
        DilithiumKeyPairGenerator kpg = new DilithiumKeyPairGenerator();
        kpg.init(new DilithiumKeyGenerationParameters(
            new java.security.SecureRandom(),
            DilithiumParameters.dilithium3
        ));
        AsymmetricCipherKeyPair kp = kpg.generateKeyPair();
        log.info("Generated CRYSTALS-Dilithium3 keypair (NIST FIPS 204, level 3)");
        return kp;
    }

    @Bean
    public DilithiumPublicKeyParameters dilithiumPublicKey(AsymmetricCipherKeyPair dilithiumKeyPair) {
        return (DilithiumPublicKeyParameters) dilithiumKeyPair.getPublic();
    }

    @Bean
    public DilithiumPrivateKeyParameters dilithiumPrivateKey(AsymmetricCipherKeyPair dilithiumKeyPair) {
        return (DilithiumPrivateKeyParameters) dilithiumKeyPair.getPrivate();
    }
}
