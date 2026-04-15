package ro.minipay.issuer.dto;

/**
 * ISO 8583 authorization response.
 *
 * Response codes:
 *   00 — Approved
 *   05 — Do Not Honor (card blocked / fraud)
 *   14 — Invalid Card Number (not found)
 *   51 — Insufficient Funds
 *   54 — Expired Card
 *   65 — Activity Limit Exceeded (daily limit)
 *   96 — System Malfunction
 */
public record AuthorizeResponse(
    String responseCode,
    String authCode,
    String holderName
) {
    public boolean isApproved() {
        return "00".equals(responseCode);
    }
}
