package com.thaiuniontour.common.exception;

import com.thaiuniontour.common.error.ErrorCode;

/**
 * 도메인/업무 규칙 위반 예외. ErrorCode를 담아 GlobalExceptionHandler가 일관 변환한다.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
