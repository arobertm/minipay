package ro.minipay.vault.crypto;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

/**
 * AES-256-GCM encryption for PAN data.
 *
 * Key lifecycle:
 *   - Generated at startup with SecureRandom (256-bit)
 *   - Stored in memory only — never persisted to disk or MiniDS
 *   - Each encryption uses a unique 96-bit IV (prepended to ciphertext)
 *
 * Ciphertext format (Base64-encoded):
 *   [12 bytes IV] [n bytes ciphertext] [16 bytes GCM auth tag]
 *
 * Security properties:
 *   - AES-256: 256-bit key, NIST approved
 *   - GCM mode: authenticated encryption (confidentiality + integrity)
 *   - Unique IV per encryption: prevents IV reuse attacks
 */
@Slf4j
@Component
public class AesGcmCipher {

    private static final String ALGORITHM  = "AES/GCM/NoPadding";
    private static final int    KEY_BITS   = 256;
    private static final int    IV_BYTES   = 12;   // 96-bit IV — GCM recommended
    private static final int    TAG_BITS   = 128;  // 128-bit auth tag

    private SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    public void init() {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES", "BC");
            kg.init(KEY_BITS, secureRandom);
            secretKey = kg.generateKey();
            log.info("AES-256-GCM key generated (in-memory, {} bits)", KEY_BITS);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate AES key", e);
        }
    }

    /**
     * Encrypt plaintext. Returns Base64(IV || ciphertext || auth_tag).
     */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM, "BC");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

            byte[] combined = new byte[IV_BYTES + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, IV_BYTES);
            System.arraycopy(ciphertext, 0, combined, IV_BYTES, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt Base64(IV || ciphertext || auth_tag). Returns original plaintext.
     */
    public String decrypt(String encryptedBase64) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedBase64);

            byte[] iv         = new byte[IV_BYTES];
            byte[] ciphertext = new byte[combined.length - IV_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_BYTES);
            System.arraycopy(combined, IV_BYTES, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM, "BC");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));

            return new String(cipher.doFinal(ciphertext));
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
