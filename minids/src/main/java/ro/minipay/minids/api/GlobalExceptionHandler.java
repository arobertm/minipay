package ro.minipay.minids.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ro.minipay.minids.raft.RaftClient;
import ro.minipay.minids.schema.SchemaValidator;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SchemaValidator.SchemaViolationException.class)
    public ResponseEntity<Map<String, String>> handleSchemaViolation(
            SchemaValidator.SchemaViolationException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "SCHEMA_VIOLATION", "message", ex.getMessage()));
    }

    @ExceptionHandler(RaftClient.RaftTimeoutException.class)
    public ResponseEntity<Map<String, String>> handleRaftTimeout(
            RaftClient.RaftTimeoutException ex) {
        log.error("Raft timeout: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "RAFT_TIMEOUT", "message", ex.getMessage()));
    }

    @ExceptionHandler(RaftClient.RaftException.class)
    public ResponseEntity<Map<String, String>> handleRaftException(
            RaftClient.RaftException ex) {
        log.error("Raft error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "RAFT_ERROR", "message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(
            IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "BAD_REQUEST", "message", ex.getMessage()));
    }
}
