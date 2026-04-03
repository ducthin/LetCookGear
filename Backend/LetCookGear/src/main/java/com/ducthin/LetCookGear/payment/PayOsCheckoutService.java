package com.ducthin.LetCookGear.payment;

import com.ducthin.LetCookGear.dto.CheckoutRequest;
import com.ducthin.LetCookGear.entity.CustomerOrder;
import com.ducthin.LetCookGear.entity.OrderItem;
import com.ducthin.LetCookGear.entity.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayOsCheckoutService {

    private static final int DESCRIPTION_MAX_LENGTH = 25;
    private static final int ITEM_NAME_MAX_LENGTH = 100;
    private static final long PAYOS_ORDER_CODE_SUFFIX_FACTOR = 1_000_000L;

    private final PayOsProperties properties;

    public PayOsCheckoutResult createCheckoutLink(
            CustomerOrder order,
            List<OrderItem> orderItems,
            User user,
            CheckoutRequest request) {
        return createCheckoutLink(
            order,
            orderItems,
            request.getReceiverName(),
            user.getEmail(),
            request.getPhone(),
            buildBuyerAddress(request.getDetail(), request.getWard(), request.getDistrict(), request.getProvince()));
        }

        public PayOsCheckoutResult createCheckoutLink(
            CustomerOrder order,
            List<OrderItem> orderItems,
            String buyerName,
            String buyerEmail,
            String buyerPhone,
            String buyerAddress) {
        validateConfiguration();

        long payOsOrderCode = generatePayOsOrderCode(order.getId());

        CreatePaymentLinkRequest payload = CreatePaymentLinkRequest.builder()
            .orderCode(payOsOrderCode)
                .amount(toLongAmount(order.getFinalAmount()))
                .description(abbreviate("LCG " + order.getOrderCode(), DESCRIPTION_MAX_LENGTH))
                .returnUrl(properties.getReturnUrl())
                .cancelUrl(properties.getCancelUrl())
            .buyerName(buyerName)
            .buyerEmail(buyerEmail)
            .buyerPhone(buyerPhone)
            .buyerAddress(buyerAddress)
                .items(toPaymentItems(orderItems))
                .build();

        try {
            CreatePaymentLinkResponse response = newPayOsClient().paymentRequests().create(payload);
            return new PayOsCheckoutResult(
                    response.getCheckoutUrl(),
                    response.getPaymentLinkId(),
                    response.getOrderCode());
        } catch (RuntimeException ex) {
            log.error("PAYOS_CREATE_LINK_FAILED orderId={} message={}", order.getId(), ex.getMessage(), ex);
            throw new IllegalArgumentException("Không thể tạo liên kết thanh toán PayOS. Vui lòng thử lại.");
        }
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new IllegalArgumentException("Cổng thanh toán PayOS hiện chưa được bật.");
        }

        if (!StringUtils.hasText(properties.getClientId())
                || !StringUtils.hasText(properties.getApiKey())
                || !StringUtils.hasText(properties.getChecksumKey())) {
            throw new IllegalArgumentException("Cấu hình PayOS chưa đầy đủ. Vui lòng liên hệ quản trị viên.");
        }

        if (!StringUtils.hasText(properties.getReturnUrl()) || !StringUtils.hasText(properties.getCancelUrl())) {
            throw new IllegalArgumentException("Thiếu URL điều hướng cho PayOS.");
        }
    }

    private PayOS newPayOsClient() {
        return new PayOS(properties.getClientId(), properties.getApiKey(), properties.getChecksumKey());
    }

    private List<PaymentLinkItem> toPaymentItems(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(item -> PaymentLinkItem.builder()
                        .name(abbreviate(item.getProductSnapshotName(), ITEM_NAME_MAX_LENGTH))
                        .quantity(item.getQuantity())
                        .price(toLongAmount(item.getUnitPrice()))
                        .build())
                .toList();
    }

    private long toLongAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán không hợp lệ.");
        }

        BigDecimal normalized = amount.setScale(0, RoundingMode.HALF_UP);
        try {
            long value = normalized.longValueExact();
            if (value <= 0) {
                throw new IllegalArgumentException("Số tiền thanh toán không hợp lệ.");
            }
            return value;
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Số tiền thanh toán không hợp lệ.");
        }
    }

    private String buildBuyerAddress(String detail, String ward, String district, String province) {
        return String.join(", ", detail, ward, district, province);
    }

    private long generatePayOsOrderCode(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Mã đơn hàng nội bộ không hợp lệ để tạo PayOS orderCode.");
        }

        long suffix = ThreadLocalRandom.current().nextLong(PAYOS_ORDER_CODE_SUFFIX_FACTOR);
        try {
            return Math.addExact(Math.multiplyExact(orderId, PAYOS_ORDER_CODE_SUFFIX_FACTOR), suffix);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Không thể tạo PayOS orderCode hợp lệ cho đơn hàng này.");
        }
    }

    private String abbreviate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "Thanh toan";
        }

        String normalized = value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    public record PayOsCheckoutResult(String checkoutUrl, String paymentLinkId, Long payOsOrderCode) {
    }
}
