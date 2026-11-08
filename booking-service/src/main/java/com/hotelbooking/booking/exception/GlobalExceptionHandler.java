package com.hotelbooking.booking.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpStatusCodeException.class)
    public ResponseEntity<Map<String, Object>> handleHttpStatusCodeException(
            HttpStatusCodeException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

        Map<String, Object> body = Map.of(
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", ex.getStatusText(),
                "path", request.getRequestURI()
        );

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> handleResourceAccessException(
            ResourceAccessException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;

        Map<String, Object> body = Map.of(
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", "Dependent service is unavailable",
                "path", request.getRequestURI()
        );

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

        Map<String, Object> body = Map.of(
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", ex.getReason() != null
                        ? ex.getReason()
                        : status.getReasonPhrase(),
                "path", request.getRequestURI()
        );

        return ResponseEntity.status(status).body(body);
    }
}
