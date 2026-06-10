package com.thaiuniontour.common.exception;

import com.thaiuniontour.common.error.ErrorCode;
import com.thaiuniontour.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * REST 전역 예외 → 공통 응답(ApiResponse.fail) 변환.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.status())
                .body(ApiResponse.fail(code.name(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .orElse(ErrorCode.COMMON_INVALID_PARAM.defaultMessage());
        ErrorCode code = ErrorCode.COMMON_INVALID_PARAM;
        return ResponseEntity.status(code.status())
                .body(ApiResponse.fail(code.name(), msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleEtc(Exception e) {
        log.error("처리되지 않은 예외", e);
        ErrorCode code = ErrorCode.COMMON_INTERNAL;
        return ResponseEntity.status(code.status())
                .body(ApiResponse.fail(code.name(), code.defaultMessage()));
    }
}
