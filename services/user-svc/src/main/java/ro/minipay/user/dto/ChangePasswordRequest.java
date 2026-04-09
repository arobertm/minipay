package ro.minipay.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for changing a user's password.
 */
public record ChangePasswordRequest(

    @NotBlank
    String currentPassword,

    @NotBlank @Size(min = 8, max = 128)
    String newPassword
) {}
