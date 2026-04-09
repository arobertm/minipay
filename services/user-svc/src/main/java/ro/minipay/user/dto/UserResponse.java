package ro.minipay.user.dto;

import ro.minipay.user.model.UserRole;
import ro.minipay.user.model.UserStatus;

/**
 * User response payload — never exposes passwordHash.
 */
public record UserResponse(
    String userId,
    String email,
    String firstName,
    String lastName,
    UserRole role,
    UserStatus status,
    String createdAt
) {}
