package com.hauly.platform.support.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Global exception handler — common error response shape.
 * Response shape: { "error": { "code": "...", "message": "...", "fields": [...] } }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 400 — Bean validation failures */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorEnvelope> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldErrorDetail> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(new ErrorEnvelope(new ErrorBody("VALIDATION_ERROR", "Request validation failed", fields)));
    }

    /** 401 — BadCredentialsException and other auth exceptions */
    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ErrorEnvelope> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorEnvelope(new ErrorBody("AUTHENTICATION_ERROR", "Authentication failed", null)));
    }

    /** 403 — Access denied */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorEnvelope> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorEnvelope(new ErrorBody("ACCESS_DENIED", "Access denied", null)));
    }

    /** 400 — Domain / Hauly-specific exceptions */
    @ExceptionHandler(HaulyException.class)
    public ResponseEntity<ErrorEnvelope> handleHauly(HaulyException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorEnvelope(new ErrorBody(ex.getCode(), ex.getMessage(), null)));
    }

    /** 400 — IllegalArgumentException from domain value objects */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorEnvelope> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorEnvelope(new ErrorBody("BAD_REQUEST", ex.getMessage(), null)));
    }

    /** 500 — Fallback: don't leak stack trace */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorEnvelope> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorEnvelope(new ErrorBody("INTERNAL_ERROR", "An unexpected error occurred", null)));
    }

    // -----------------------------------------------------------------------

    public record ErrorEnvelope(ErrorBody error) {}

    public record ErrorBody(String code, String message, List<FieldErrorDetail> fields) {}

    public record FieldErrorDetail(String field, String message) {}
}
