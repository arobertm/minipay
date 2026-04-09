package ro.minipay.session.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.minipay.session.dto.CreateSessionRequest;
import ro.minipay.session.dto.SessionResponse;
import ro.minipay.session.service.SessionService;
import ro.minipay.session.service.SessionService.SessionNotFoundException;

import java.util.List;
import java.util.Map;

/**
 * REST API for device session management.
 *
 * POST   /sessions                  — create session (internal, called by auth-svc)
 * GET    /sessions/{id}             — get session by ID
 * GET    /sessions?userId=          — list active sessions for user
 * POST   /sessions/{id}/touch       — update lastSeenAt
 * DELETE /sessions/{id}             — revoke single session (logout)
 * DELETE /sessions?userId=          — revoke all sessions for user
 */
@Slf4j
@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    public ResponseEntity<SessionResponse> create(@Valid @RequestBody CreateSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.createSession(request));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionResponse> getById(@PathVariable String sessionId) {
        return ResponseEntity.ok(sessionService.getSession(sessionId));
    }

    @GetMapping
    public ResponseEntity<List<SessionResponse>> getByUser(@RequestParam String userId) {
        return ResponseEntity.ok(sessionService.getUserSessions(userId));
    }

    @PostMapping("/{sessionId}/touch")
    public ResponseEntity<Void> touch(@PathVariable String sessionId) {
        sessionService.touch(sessionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> revoke(@PathVariable String sessionId) {
        sessionService.revokeSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> revokeAll(@RequestParam String userId) {
        int count = sessionService.revokeAllUserSessions(userId);
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "revokedCount", count,
            "message", "All sessions revoked"
        ));
    }

    // --- exception handlers ---

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(SessionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", ex.getMessage()));
    }
}
