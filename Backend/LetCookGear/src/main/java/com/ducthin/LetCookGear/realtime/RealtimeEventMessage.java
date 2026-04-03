package com.ducthin.LetCookGear.realtime;

public record RealtimeEventMessage(
        RealtimeEventType type,
        String userEmail,
        Long orderId,
        String orderCode,
        String paymentStatus,
        String orderStatus,
        Integer totalItems,
        String message) {
}