package com.ducthin.LetCookGear.realtime;

import com.ducthin.LetCookGear.entity.CustomerOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RealtimeEventPublisher {

    private static final String REALTIME_TOPIC = "/topic/realtime";

    private final SimpMessagingTemplate messagingTemplate;

    public void publishCartUpdated(String userEmail, int totalItems) {
        publish(new RealtimeEventMessage(
                RealtimeEventType.CART_UPDATED,
                userEmail,
                null,
                null,
                null,
                null,
                totalItems,
                null));
    }

    public void publishPaymentPaid(CustomerOrder order) {
        String userEmail = order.getUser() == null ? null : order.getUser().getEmail();
        publish(new RealtimeEventMessage(
                RealtimeEventType.PAYMENT_PAID,
                userEmail,
                order.getId(),
                order.getOrderCode(),
                order.getPaymentStatus().name(),
                order.getStatus().name(),
                null,
                "Don " + order.getOrderCode() + " da thanh toan thanh cong."));
    }

    private void publish(RealtimeEventMessage eventMessage) {
        messagingTemplate.convertAndSend(REALTIME_TOPIC, eventMessage);
    }
}