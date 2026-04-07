package ro.minipay.minids.schema;

import java.util.Set;

/**
 * Schema MiniDS — Object Classes disponibile.
 *
 * Inspirat din schema LDAP a PingDS:
 *   - fiecare objectClass defineste ce atribute sunt permise/obligatorii
 *   - validarea se face la CREATE si MODIFY
 *
 * PingDS real foloseste schema LDAP standard (RFC 4519) + extensii proprii.
 * Noi folosim o schema simplificata JSON-based.
 */
public enum ObjectClass {

    /**
     * Utilizator al sistemului MiniPay.
     * Echivalent: inetOrgPerson + custom schema in PingDS
     */
    MINIPAY_USER(
        "minipayUser",
        Set.of("uid", "cn", "mail"),                          // obligatorii
        Set.of("givenName", "sn", "passwordHash",             // optionale
               "totpSecret", "roles", "accountStatus",
               "phone", "lastLogin", "failedLogins")
    ),

    /**
     * Token OAuth2 — stocat in CTS (Core Token Service).
     * Echivalent: fr-idrepo-coreTokenType din PingDS CTS schema
     */
    CORE_TOKEN(
        "coreToken",
        Set.of("coreTokenId", "coreTokenType", "coreTokenExpiry"),
        Set.of("coreTokenUserId", "coreTokenClientId",
               "coreTokenValue", "coreTokenScope",
               "coreTokenGrantType", "coreTokenString")
    ),

    /**
     * Sesiune activa a unui utilizator.
     */
    SESSION(
        "minipaySession",
        Set.of("sessionId", "userId", "expiresAt"),
        Set.of("ipAddress", "userAgent", "createdAt",
               "lastAccessed", "mfaVerified")
    ),

    /**
     * OAuth2 Client inregistrat (aplicatia merchant).
     * Echivalent: agentType=OAuth2Client in PingDS
     */
    OAUTH2_CLIENT(
        "oauth2Client",
        Set.of("clientId", "clientSecret", "redirectUris"),
        Set.of("grantTypes", "scopes", "clientName",
               "accessTokenTtl", "refreshTokenTtl",
               "merchantId", "status")
    ),

    /**
     * Card tokenizat — vault entry.
     * Nou — nu exista echivalent in PingDS standard.
     */
    CARD_VAULT(
        "cardVault",
        Set.of("dpan", "maskedPan", "expiryEncrypted"),
        Set.of("panEncrypted", "cvvHash", "cardholderName",
               "tokenExpiry", "merchantId", "userId")
    ),

    /**
     * Container organizational (OU).
     * Echivalent: organizationalUnit in LDAP
     */
    ORGANIZATIONAL_UNIT(
        "organizationalUnit",
        Set.of("ou"),
        Set.of("description")
    ),

    /**
     * Radacina arborelui DIT (Directory Information Tree).
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
