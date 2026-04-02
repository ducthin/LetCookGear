package com.ducthin.LetCookGear.dto;

import com.ducthin.LetCookGear.entity.enums.OrderStatus;
import com.ducthin.LetCookGear.entity.enums.PaymentMethod;
import com.ducthin.LetCookGear.entity.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderResponse {
    private Long orderId;
    private String orderCode;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private BigDecimal totalAmount;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private LocalDateTime placedAt;
    private List<OrderItemResponse> items;
}
