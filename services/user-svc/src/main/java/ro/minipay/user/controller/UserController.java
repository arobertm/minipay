package ro.minipay.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.minipay.user.dto.*;
import ro.minipay.user.service.UserService;
import ro.minipay.user.service.UserService.UserNotFoundException;

import java.util.Map;

/**
 * REST API for user management.
 *
 * Endpoints:
 *   POST   /users                           — register (public)
 *   GET    /users/{userId}                  — get by ID (authenticated)
 *   GET    /users?email=...                 — find by email (authenticated)
 *   PUT    /users/{userId}                  — update profile (authenticated)
 *   POST   /users/{userId}/change-password  — change password (authenticated)
 *   DELETE /users/{userId}                  — soft-delete / GDPR erasure (ADMIN only)
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getById(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    @GetMapping
    public ResponseEntity<UserResponse> getByEmail(@RequestParam String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> update(@PathVariable String userId,
                                               @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(userId, request));
    }

    @PostMapping("/{userId}/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@PathVariable String userId,
                                                              @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('SCOPE_admin') or authentication.name == #userId")
    public ResponseEntity<Void> delete(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    // --- exception handlers ---

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }
}
