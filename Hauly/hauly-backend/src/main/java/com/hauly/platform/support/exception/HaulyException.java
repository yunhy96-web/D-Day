package com.hauly.platform.support.exception;

/**
 * Base class for Hauly domain exceptions.
 * Extend this for domain-specific errors in future bounded contexts.
 */
public class HaulyException extends RuntimeException {

    private final String code;

    public HaulyException(String code, String message) {
        super(message);
        this.code = code;
    }

    public HaulyException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
