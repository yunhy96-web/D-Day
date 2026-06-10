package com.thaiuniontour.common.error;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드 체계 (04-api.md). <DOMAIN>_<REASON> + HTTP status 매핑.
 */
public enum ErrorCode {
    COMMON_INVALID_PARAM(HttpStatus.BAD_REQUEST, "요청이 올바르지 않습니다."),
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    COMMON_NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다."),
    RESERVATION_SOLD_OUT(HttpStatus.CONFLICT, "해당 날짜는 매진되었습니다."),
    RESERVATION_INVALID_STATE(HttpStatus.CONFLICT, "처리할 수 없는 예약 상태입니다."),
    PAYMENT_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 결제입니다."),
    COMMON_INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
