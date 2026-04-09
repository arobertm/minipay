package ro.minipay.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ro.minipay.user.dto.ChangePasswordRequest;
import ro.minipay.user.dto.CreateUserRequest;
import ro.minipay.user.dto.UpdateUserRequest;
import ro.minipay.user.dto.UserResponse;
import ro.minipay.user.model.UserRole;
import ro.minipay.user.model.UserStatus;
import ro.minipay.user.repository.MiniDSUserRepository;
import ro.minipay.user.shared.minids.MiniDSClientService.Entry;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Business logic for user management.
 *
 * Password hashing: Argon2id (OWASP recommended since 2023).
 * Storage: MiniDS directory server (ou=users,dc=minipay,dc=ro).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final MiniDSUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Register a new user.
     * Password is hashed with Argon2id before storage — plaintext never persisted.
     */
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered: " + request.email());
        }

        String userId = UUID.randomUUID().toString();
        String passwordHash = passwordEncoder.encode(request.password());
        UserRole role = request.role() != null ? request.role() : UserRole.USER;
        String createdAt = Instant.now().toString();

        userRepository.save(userId, request.email(), passwordHash,
            request.firstName(), request.lastName(), role, UserStatus.ACTIVE, createdAt);

        log.info("User created: {} ({})", userId, request.email());
        return new UserResponse(userId, request.email(), request.firstName(),
            request.lastName(), role, UserStatus.ACTIVE, createdAt);
    }

    /**
     * Retrieve user by ID.
     */
    public UserResponse getUser(String userId) {
        Entry entry = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        return toResponse(entry);
    }

    /**
     * Find user by email address.
     */
    public UserResponse getUserByEmail(String email) {
        Entry entry = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("email=" + email));
        return toResponse(entry);
    }

    /**
     * Update user profile (firstName / lastName).
     */
    public UserResponse updateUser(String userId, UpdateUserRequest request) {
        // verify exists
        Entry current = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        Map<String, String> updates = new HashMap<>();
        if (request.firstName() != null) updates.put("givenName", request.firstName());
        if (request.lastName() != null) updates.put("sn", request.lastName());

        if (!updates.isEmpty()) {
            userRepository.update(userId, updates);
        }

        // re-fetch updated entry
        Entry updated = userRepository.findById(userId).orElse(current);
        return toResponse(updated);
    }

    /**
     * Change password — verifies current password before updating.
     * New password is hashed with Argon2id.
     */
    public void changePassword(String userId, ChangePasswordRequest request) {
        Entry entry = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        String storedHash = entry.getAttribute("userPassword");
        if (!passwordEncoder.matches(request.currentPassword(), storedHash)) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        String newHash = passwordEncoder.encode(request.newPassword());
        userRepository.update(userId, Map.of("userPassword", newHash));
        log.info("Password changed for user: {}", userId);
    }

    /**
     * Soft-delete user (GDPR Art.17 — right to erasure).
     * Sets status=DELETED and clears PII fields.
     */
    public void deleteUser(String userId) {
        userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        Map<String, String> erasure = new HashMap<>();
        erasure.put("status", UserStatus.DELETED.name());
        erasure.put("givenName", "[deleted]");
        erasure.put("sn", "[deleted]");
        erasure.put("mail", "deleted-" + userId + "@minipay.ro");
        erasure.put("userPassword", "");

        userRepository.update(userId, erasure);
        log.info("User soft-deleted (GDPR erasure): {}", userId);
    }

    // --- helpers ---

    private UserResponse toResponse(Entry entry) {
        Map<String, String> a = entry.getAttributes();
        return new UserResponse(
            a.getOrDefault("uid", ""),
            a.getOrDefault("mail", ""),
            a.getOrDefault("givenName", ""),
            a.getOrDefault("sn", ""),
            parseRole(a.get("role")),
            parseStatus(a.get("status")),
            a.getOrDefault("createdAt", "")
        );
    }

    private UserRole parseRole(String value) {
        try { return UserRole.valueOf(value); } catch (Exception e) { return UserRole.USER; }
    }

    private UserStatus parseStatus(String value) {
        try { return UserStatus.valueOf(value); } catch (Exception e) { return UserStatus.ACTIVE; }
    }

    // --- exceptions ---

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String id) {
            super("User not found: " + id);
        }
    }
}
