package ro.minipay.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ro.minipay.user.model.UserRole;

/**
 * Request body for user registration.
 */
public record CreateUserRequest(

    @NotBlank @Email
    String email,

    @NotBlank @Size(min = 8, max = 128)
    String password,

    @NotBlank @Size(max = 64)
    String firstName,

    @NotBlank @Size(max = 64)
    String lastName,

    UserRole role   // defaults to USER if null
) {}
