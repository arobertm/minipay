package ro.minipay.user.dto;

import jakarta.validation.constraints.Size;

/**
 * Request body for updating user profile (partial update — null fields are ignored).
 */
public record UpdateUserRequest(

    @Size(max = 64)
    String firstName,

    @Size(max = 64)
    String lastName
) {}
