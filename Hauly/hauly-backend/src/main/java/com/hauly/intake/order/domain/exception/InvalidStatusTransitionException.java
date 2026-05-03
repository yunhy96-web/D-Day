package com.hauly.intake.order.domain.exception;

import com.hauly.platform.support.exception.HaulyException;

/**
 * Thrown when an order is asked to transition to a status that the state machine forbids.
 * Maps to HTTP 400 via GlobalExceptionHandler with code INVALID_STATUS_TRANSITION.
 */
public class InvalidStatusTransitionException extends HaulyException {

    public InvalidStatusTransitionException(String message) {
        super("INVALID_STATUS_TRANSITION", message);
    }
}
