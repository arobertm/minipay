package ro.minipay.vault.crypto;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates DPAN (Digital PAN / payment token) from a real PAN.
 *
 * EMV Tokenization spec (EMVCo Token spec v2.0):
 *   - DPAN preserves the BIN (first 6 digits) for routing
 *   - DPAN has the same length as original PAN (16 digits for Visa/MC)
 *   - DPAN passes Luhn check (required by payment networks)
 *   - DPAN is statistically indistinguishable from a real PAN
 *
 * Format:
 *   [6 digits BIN] [9 random digits] [1 Luhn check digit]
 */
@Component
public class DpanGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a DPAN preserving the BIN of the original PAN.
     *
     * @param pan original PAN (13-19 digits)
     * @return 16-digit Luhn-valid DPAN
     */
    public String generate(String pan) {
        String bin = pan.substring(0, 6);

        // Generate 9 random digits (positions 6-14)
        StringBuilder sb = new StringBuilder(bin);
        for (int i = 0; i < 9; i++) {
            sb.append(secureRandom.nextInt(10));
        }

        // Calculate and append Luhn check digit
        sb.append(luhnCheckDigit(sb.toString()));
        return sb.toString();
    }

    /**
     * Validate that a card number passes the Luhn check.
     * Used to verify generated DPANs and incoming PANs.
     */
    public boolean isValidLuhn(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = number.charAt(i) - '0';
            if (alternate) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    /**
     * Calculate the Luhn check digit for a partial number (without check digit).
     */
    private int luhnCheckDigit(String partialNumber) {
        // Append a 0 placeholder and calculate what makes the full number valid
        for (int check = 0; check <= 9; check++) {
            if (isValidLuhn(partialNumber + check)) {
                return check;
            }
        }
        return 0;
    }
}
