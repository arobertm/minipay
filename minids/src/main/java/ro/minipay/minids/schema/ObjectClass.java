package ro.minipay.minids.schema;

import java.util.Set;

/**
 * MiniDS Schema — available Object Classes.
 *
 * Inspired by the LDAP schema in PingDS:
 *   - each objectClass defines which attributes are allowed/required
 *   - validation is performed on CREATE and MODIFY
 *
 * Real PingDS uses the standard LDAP schema (RFC 4519) + its own extensions.
 * We use a simplified JSON-based schema.
 */
public enum ObjectClass {

    /**
     * MiniPay system user.
     * Equivalent: inetOrgPerson + custom schema in PingDS
     */
    MINIPAY_USER(
        "minipayUser",
        Set.of("uid", "cn", "mail"),                          // obligatorii
        Set.of("givenName", "sn", "passwordHash",             // optionale
               "totpSecret", "roles", "accountStatus",
               "phone", "lastLogin", "failedLogins")
    ),

    /**
     * OAuth2 Token — stored in CTS (Core Token Service).
     * Equivalent: fr-idrepo-coreTokenType in PingDS CTS schema
     */
    CORE_TOKEN(
        "coreToken",
        Set.of("coreTokenId", "coreTokenType", "coreTokenExpiry"),
        Set.of("coreTokenUserId", "coreTokenClientId",
               "coreTokenValue", "coreTokenScope",
               "coreTokenGrantType", "coreTokenString")
    ),

    /**
     * An active user session.
     */
    SESSION(
        "minipaySession",
        Set.of("sessionId", "userId", "expiresAt"),
        Set.of("ipAddress", "userAgent", "createdAt",
               "lastAccessed", "mfaVerified")
    ),

    /**
     * Registered OAuth2 Client (the merchant application).
     * Equivalent: agentType=OAuth2Client in PingDS
     */
    OAUTH2_CLIENT(
        "oauth2Client",
        Set.of("clientId", "clientSecret", "redirectUris"),
        Set.of("grantTypes", "scopes", "clientName",
               "accessTokenTtl", "refreshTokenTtl",
               "merchantId", "status")
    ),

    /**
     * OAuth2 Authorization Consent — user approval of scopes to a client.
     * New — specific to OAuth2 authorization flow.
     *
     * Example DN: cn=test-client+john.doe,ou=consents,dc=minipay,dc=ro
     * User "john.doe" approved client "test-client" for certain scopes.
     */
    OAUTH2_CONSENT(
        "oauth2Consent",
        Set.of("clientId", "principalName", "scopes"),
        Set.of("expiresAt", "createdAt", "authorities")
    ),

    /**
     * Tokenized card — vault entry.
     * New — no equivalent in standard PingDS.
     */
    CARD_VAULT(
        "cardVault",
        Set.of("dpan", "maskedPan", "expiryEncrypted"),
        Set.of("panEncrypted", "cvvHash", "cardholderName",
               "tokenExpiry", "merchantId", "userId")
    ),

    /**
     * Organizational container (OU).
     * Equivalent: organizationalUnit in LDAP
     */
    ORGANIZATIONAL_UNIT(
        "organizationalUnit",
        Set.of("ou"),
        Set.of("description")
    ),

    /**
     * Root of the DIT (Directory Information Tree).
     */
    DOMAIN_COMPONENT(
        "domain",
        Set.of("dc"),
        Set.of("description")
    );

    public final String name;
    public final Set<String> requiredAttributes;
    public final Set<String> optionalAttributes;

    ObjectClass(String name, Set<String> required, Set<String> optional) {
        this.name = name;
        this.requiredAttributes = required;
        this.optionalAttributes = optional;
    }

    public static ObjectClass fromName(String name) {
        for (ObjectClass oc : values()) {
            if (oc.name.equalsIgnoreCase(name)) return oc;
        }
        throw new IllegalArgumentException("Unknown objectClass: " + name);
    }

    public boolean isAttributeAllowed(String attr) {
        return requiredAttributes.contains(attr) || optionalAttributes.contains(attr);
    }
}
