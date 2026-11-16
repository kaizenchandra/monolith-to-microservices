package demo.platform;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.dao.DataIntegrityViolationException;

@RestControllerAdvice
public class Errors {
    @ExceptionHandler(org.springframework.dao.EmptyResultDataAccessException.class)
    ResponseEntity<?> missing() {
        return ResponseEntity.status(404).body(java.util.Map.of("code", "NOT_FOUND"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> invalid(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(java.util.Map.of("code", "INVALID_REQUEST", "message", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<?> conflict(IllegalStateException e) {
        return ResponseEntity.status(409).body(java.util.Map.of("code", "CONFLICT", "message", e.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> integrity() {
        return ResponseEntity.status(409).body(java.util.Map.of("code", "CONSTRAINT_CONFLICT"));
    }

    @ExceptionHandler(org.springframework.web.client.HttpClientErrorException.class)
    ResponseEntity<?> client(org.springframework.web.client.HttpClientErrorException e) {
        return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(e.getResponseBodyAsString());
    }

    @ExceptionHandler(org.springframework.web.client.RestClientException.class)
    ResponseEntity<?> downstream() {
        return ResponseEntity.status(503).body(java.util.Map.of("code", "DEPENDENCY_UNAVAILABLE"));
    }
}
