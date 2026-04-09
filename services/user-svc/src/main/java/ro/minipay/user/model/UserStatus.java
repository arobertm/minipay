package ro.minipay.user.model;

/**
 * Lifecycle status of a user account.
 */
public enum UserStatus {
    ACTIVE,
    SUSPENDED,  // temporarily locked (fraud / admin action)
    DELETED     // soft-deleted (GDPR erasure)
}
