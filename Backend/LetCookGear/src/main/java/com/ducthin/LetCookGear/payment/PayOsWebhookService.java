package com.ducthin.LetCookGear.payment;

import com.ducthin.LetCookGear.entity.CustomerOrder;
import com.ducthin.LetCookGear.entity.Payment;
import com.ducthin.LetCookGear.entity.enums.OrderStatus;
import com.ducthin.LetCookGear.entity.enums.PaymentStatus;
import com.ducthin.LetCookGear.realtime.RealtimeEventPublisher;
import com.ducthin.LetCookGear.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.payos.PayOS;
import vn.payos.model.webhooks.WebhookData;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayOsWebhookService {

    private static final DateTimeFormatter PAYOS_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long PAYOS_ORDER_CODE_SUFFIX_FACTOR = 1_000_000L;

    private final PayOsProperties properties;
    private final PaymentRepository paymentRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;

    @Transactional
    public boolean handleWebhook(Map<String, Object> payload) {
        validateConfiguration();

        WebhookData data = verifyPayload(payload);
        Optional<Payment> paymentOptional = resolvePayment(data);

        if (paymentOptional.isEmpty()) {
            log.warn(
                    "PAYOS_WEBHOOK_PAYMENT_NOT_FOUND paymentLinkId={} orderCode={}",
                    data.getPaymentLinkId(),
                    data.getOrderCode());
            return false;
        }

        Payment payment = paymentOptional.get();
        CustomerOrder order = payment.getOrder();

        if (StringUtils.hasText(data.getPaymentLinkId())) {
            payment.setTransactionRef(data.getPaymentLinkId());
        }

        if (isSuccessfulCode(data.getCode())) {
            payment.setStatus(PaymentStatus.PAID);
            if (payment.getPaidAt() == null) {
                payment.setPaidAt(parseTransactionDateTime(data.getTransactionDateTime()).orElse(LocalDateTime.now()));
            }
            order.setPaymentStatus(PaymentStatus.PAID);
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.CONFIRMED);
            }
            realtimeEventPublisher.publishPaymentPaid(order);

            log.info(
                    "PAYOS_WEBHOOK_PAID orderId={} orderCode={} paymentLinkId={}",
                    order.getId(),
                    order.getOrderCode(),
                    payment.getTransactionRef());
            return true;
        }

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            payment.setStatus(PaymentStatus.FAILED);
            order.setPaymentStatus(PaymentStatus.FAILED);
        }

        log.info(
                "PAYOS_WEBHOOK_NON_SUCCESS orderId={} orderCode={} dataCode={} dataDesc={}",
                order.getId(),
                order.getOrderCode(),
                data.getCode(),
                data.getDesc());

        return false;
    }

    private Optional<Payment> resolvePayment(WebhookData data) {
        if (StringUtils.hasText(data.getPaymentLinkId())) {
            Optional<Payment> byPaymentLinkId = paymentRepository.findTopByTransactionRefOrderByCreatedAtDesc(data.getPaymentLinkId());
            if (byPaymentLinkId.isPresent()) {
                return byPaymentLinkId;
            }
        }

        Long payOsOrderCode = data.getOrderCode();
        if (payOsOrderCode == null) {
            return Optional.empty();
        }

        Optional<Payment> byDirectOrderId = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(payOsOrderCode);
        if (byDirectOrderId.isPresent()) {
            return byDirectOrderId;
        }

        return extractOrderIdFromPayOsOrderCode(payOsOrderCode)
                .flatMap(paymentRepository::findTopByOrderIdOrderByCreatedAtDesc);
    }

    private Optional<Long> extractOrderIdFromPayOsOrderCode(Long payOsOrderCode) {
        if (payOsOrderCode == null || payOsOrderCode <= 0) {
            return Optional.empty();
        }

        long extractedOrderId = payOsOrderCode / PAYOS_ORDER_CODE_SUFFIX_FACTOR;
        if (extractedOrderId <= 0) {
            return Optional.empty();
        }

        return Optional.of(extractedOrderId);
    }

    private WebhookData verifyPayload(Map<String, Object> payload) {
        try {
            return newPayOsClient().webhooks().verify(payload);
        } catch (RuntimeException ex) {
            log.warn("PAYOS_WEBHOOK_VERIFY_FAILED message={}", ex.getMessage());
            throw new IllegalArgumentException("Webhook PayOS không hợp lệ hoặc sai chữ ký.");
        }
    }

    private Optional<LocalDateTime> parseTransactionDateTime(String transactionDateTime) {
        if (!StringUtils.hasText(transactionDateTime)) {
            return Optional.empty();
        }

        try {
            return Optional.of(LocalDateTime.parse(transactionDateTime, PAYOS_TIMESTAMP_FORMAT));
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }

    private boolean isSuccessfulCode(String code) {
        return "00".equals(code == null ? "" : code.trim());
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new IllegalArgumentException("Webhook PayOS chưa được bật.");
        }

        if (!StringUtils.hasText(properties.getClientId())
                || !StringUtils.hasText(properties.getApiKey())
                || !StringUtils.hasText(properties.getChecksumKey())) {
            throw new IllegalArgumentException("Cấu hình PayOS chưa đầy đủ để xác thực webhook.");
        }
    }

    private PayOS newPayOsClient() {
        return new PayOS(properties.getClientId(), properties.getApiKey(), properties.getChecksumKey());
    }
}
