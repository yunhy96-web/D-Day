package com.dday.sunfix.common;

import com.dday.sunfix.security.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** JSON API 오류는 {code} 만. 화면은 code 로 라벨 err.{code} 를 찾아 보여준다. */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(BadRequest.class)
    public ResponseEntity<Map<String, String>> bad(BadRequest e) {
        return ResponseEntity.badRequest().body(Map.of("code", e.getMessage()));
    }
    @ExceptionHandler(NotFound.class)
    public ResponseEntity<Map<String, String>> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("code", "notFound"));
    }
    @ExceptionHandler(RateLimiter.TooManyRequests.class)
    public ResponseEntity<Map<String, String>> tooMany() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("code", "rate"));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> badJson() {
        return ResponseEntity.badRequest().body(Map.of("code", "json"));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> other(Exception e) {
        if (e instanceof ErrorResponse er)
            return ResponseEntity.status(er.getStatusCode()).body(Map.of("code", String.valueOf(er.getStatusCode().value())));
        log.error("unhandled", e);
        return ResponseEntity.internalServerError().body(Map.of("code", "error"));
    }
}
