package ro.minipay.tds.model;

import java.time.Instant;

/**
 * 3DS2 authentication result.
 *
 * transStatus values (EMVCo spec):
 *   Y — Authentication Successful (frictionless)
 *   C — Challenge Required
 *   N — Not Authenticated
 *   U — Authentication Could Not Be Performed
 */
public record AuthenticationResult(
    String  acsTransID,
    String  threeDSServerTransID,
    String  transStatus,         // Y | C | N | U
    String  transStatusReason,
    String  authenticationValue, // CAVV — 28-char Base64 (simulated)
    String  eci,                 // 05=Y,07=A,06=N (Visa codes)
    String  challengeURL,        // non-null only when transStatus=C
    Instant authenticatedAt
) {}
