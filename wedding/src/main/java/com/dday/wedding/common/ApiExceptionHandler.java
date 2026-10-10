package com.dday.wedding.common;

import com.dday.wedding.security.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.ErrorResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.Map;

/** 스택트레이스 노출 금지. {code} 만. */
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(BadRequest.class)
    public ResponseEntity<Map<String, String>> bad(BadRequest e) {
        return ResponseEntity.badRequest().body(Map.of("code", e.getMessage()));
    }
    @ExceptionHandler(RateLimiter.TooManyRequests.class)
    public ResponseEntity<Map<String, String>> tooMany() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("code", "rate"));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> badJson() {
        return ResponseEntity.badRequest().body(Map.of("code", "json"));
    }
    /** 404(리소스 없음)·405 등 스프링이 상태를 아는 예외(ErrorResponse 구현)는 그 상태로, 나머지는 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> other(Exception e) {
        if (e instanceof ErrorResponse er)
            return ResponseEntity.status(er.getStatusCode()).body(Map.of("code", String.valueOf(er.getStatusCode().value())));
        return ResponseEntity.internalServerError().body(Map.of("code", "error"));
    }
}
