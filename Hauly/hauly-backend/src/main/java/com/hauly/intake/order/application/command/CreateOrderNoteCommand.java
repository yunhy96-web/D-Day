package com.hauly.intake.order.application.command;

public record CreateOrderNoteCommand(Long orderId, String body) {}
