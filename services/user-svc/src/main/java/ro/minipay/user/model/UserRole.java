package ro.minipay.user.model;

/**
 * RBAC roles for MiniPay users.
 */
public enum UserRole {
    USER,       // standard cardholder
    MERCHANT,   // merchant accepting payments
    ADMIN       // platform administrator
}
