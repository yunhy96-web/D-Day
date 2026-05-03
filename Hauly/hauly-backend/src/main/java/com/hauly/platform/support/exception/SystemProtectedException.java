package com.hauly.platform.support.exception;

/**
 * Thrown when an operation attempts to modify or delete a system-protected resource.
 * Maps to HTTP 409 CONFLICT — the operation conflicts with the resource's protected state.
 */
public class SystemProtectedException extends HaulyException {

    public SystemProtectedException(String message) {
        super("SYSTEM_PROTECTED", message);
    }
}
