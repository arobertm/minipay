package ro.minipay.auth.crypto;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.pqc.crypto.crystals.Dilithium;
import org.bouncycastle.pqc.crypto.crystals.DilithiumParameters;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

/**
 * Dilithium3 Key Provider for post-quantum cryptography.
 *
 * Generates and manages CRYSTALS-Dilithium3 keypairs used for JWT signing.
 * Dilithium is a lattice-based digital signature scheme selected by NIST
 * as part of the post-quantum cryptography standardization project (FIPS 204).
 *
 * Key generation is expensive, so we generate once on startup and reuse.
 */
@Slf4j
@Component
public class DilithiumKeyProvider {

    private AsymmetricCipherKeyPair keyPair;
    private byte[] publicKeyEncoded;

    public DilithiumKeyProvider() {
        generateKeyPair();
    }

    /**
     * Generate a new Dilithium3 keypair.
     * Called once on startup.
     */
    private void generateKeyPair() {
        try {
            log.info("Generating CRYSTALS-Dilithium3 keypair...");

            Dilithium keygen = new Dilithium(DilithiumParameters.dilithium3);
            this.keyPair = keygen.generateKeyPair();

            // Encode public key for /oauth2/jwks endpoint
            org.bouncycastle.pqc.crypto.crystals.DilithiumPublicKeyParameters pubKeyParams =
                (org.bouncycastle.pqc.crypto.crystals.DilithiumPublicKeyParameters) keyPair.getPublic();
            this.publicKeyEncoded = pubKeyParams.getEncoded();

            log.info("Dilithium3 keypair generated successfully");
        } catch (Exception e) {
            log.error("Failed to generate Dilithium3 keypair", e);
            throw new RuntimeException("Dilithium key generation failed", e);
        }
    }

    /**
     * Get the public key (for JWT verification / jwks endpoint).
     */
    public org.bouncycastle.pqc.crypto.crystals.DilithiumPublicKeyParameters getPublicKey() {
        return (org.bouncycastle.pqc.crypto.crystals.DilithiumPublicKeyParameters) keyPair.getPublic();
    }

    /**
     * Get the private key (for JWT signing).
     */
    public org.bouncycastle.pqc.crypto.crystals.DilithiumPrivateKeyParameters getPrivateKey() {
        return (org.bouncycastle.pqc.crypto.crystals.DilithiumPrivateKeyParameters) keyPair.getPrivate();
    }

    /**
     * Get encoded public key bytes (for /oauth2/jwks response).
     */
    public byte[] getEncodedPublicKey() {
        return publicKeyEncoded;
    }

    /**
     * Get algorithm name.
     */
    public String getAlgorithm() {
        return "DILITHIUM3";
    }
}
