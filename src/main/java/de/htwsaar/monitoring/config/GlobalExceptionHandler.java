package de.htwsaar.monitoring.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidJson(
            HttpMessageNotReadableException exception
    ) {
        log.warn(
                "Rejected request with invalid JSON payload: {}",
                exception.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "invalid_json",
                "message", "The request body must contain valid JSON.",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedError(
            Exception exception
    ) {
        log.error("Unexpected application error", exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "internal_error",
                "message", "An unexpected server error occurred.",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}