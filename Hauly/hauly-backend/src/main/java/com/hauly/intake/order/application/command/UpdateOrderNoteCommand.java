package com.hauly.intake.order.application.command;

public record UpdateOrderNoteCommand(Long orderId, Long noteId, String body) {}
