package com.ducthin.LetCookGear.service;

import com.ducthin.LetCookGear.dto.CheckoutRequest;
import com.ducthin.LetCookGear.dto.OrderItemResponse;
import com.ducthin.LetCookGear.dto.OrderResponse;
import com.ducthin.LetCookGear.entity.Address;
import com.ducthin.LetCookGear.entity.Cart;
import com.ducthin.LetCookGear.entity.CartItem;
import com.ducthin.LetCookGear.entity.CustomerOrder;
import com.ducthin.LetCookGear.entity.Inventory;
import com.ducthin.LetCookGear.entity.OrderItem;
import com.ducthin.LetCookGear.entity.Payment;
import com.ducthin.LetCookGear.entity.Shipment;
import com.ducthin.LetCookGear.entity.User;
import com.ducthin.LetCookGear.entity.enums.CartStatus;
import com.ducthin.LetCookGear.entity.enums.OrderStatus;
import com.ducthin.LetCookGear.entity.enums.PaymentMethod;
import com.ducthin.LetCookGear.entity.enums.PaymentStatus;
import com.ducthin.LetCookGear.entity.enums.ShipmentStatus;
import com.ducthin.LetCookGear.payment.PayOsCheckoutService;
import com.ducthin.LetCookGear.realtime.RealtimeEventPublisher;
import com.ducthin.LetCookGear.repository.AddressRepository;
import com.ducthin.LetCookGear.repository.CustomerOrderRepository;
import com.ducthin.LetCookGear.repository.InventoryRepository;
import com.ducthin.LetCookGear.repository.OrderItemRepository;
import com.ducthin.LetCookGear.repository.PaymentRepository;
import com.ducthin.LetCookGear.repository.ShipmentRepository;
import com.ducthin.LetCookGear.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final BigDecimal DEFAULT_SHIPPING_FEE = new BigDecimal("30000");

    private final UserRepository userRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final ShipmentRepository shipmentRepository;
    private final AddressRepository addressRepository;
    private final InventoryRepository inventoryRepository;
    private final CartService cartService;
    private final PayOsCheckoutService payOsCheckoutService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    @Transactional
    public OrderResponse checkout(String email, CheckoutRequest request) {
        User user = getUserByEmail(email);
        Cart cart = cartService.getActiveCartOrThrow(user.getId());

        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng đang trống");
        }

        for (CartItem item : cart.getItems()) {
            Inventory inventory = inventoryRepository
                    .findByVariantId(item.getVariant().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tồn kho cho phiên bản sản phẩm"));
            if (inventory.getQuantityAvailable() < item.getQuantity()) {
                throw new IllegalArgumentException("Không đủ tồn kho cho phiên bản: " + item.getVariant().getSku());
            }
        }

        BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CustomerOrder order = new CustomerOrder();
        order.setUser(user);
        order.setOrderCode(generateOrderCode());
        order.setTotalAmount(subtotal);
        order.setShippingFee(DEFAULT_SHIPPING_FEE);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFinalAmount(subtotal.add(DEFAULT_SHIPPING_FEE));
        order.setPaymentStatus(resolvePaymentStatus(request.getPaymentMethod()));
        order.setPlacedAt(LocalDateTime.now());
        CustomerOrder savedOrder = customerOrderRepository.save(order);

        List<OrderItem> orderItems = cart.getItems().stream().map(cartItem -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setVariant(cartItem.getVariant());
            orderItem.setProductSnapshotName(cartItem.getVariant().getProduct().getName());
            orderItem.setSkuSnapshot(cartItem.getVariant().getSku());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setLineTotal(cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            return orderItemRepository.save(orderItem);
        }).toList();

        for (CartItem item : cart.getItems()) {
            Inventory inventory = inventoryRepository
                    .findByVariantId(item.getVariant().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tồn kho cho phiên bản sản phẩm"));
            inventory.setQuantityAvailable(inventory.getQuantityAvailable() - item.getQuantity());
            inventoryRepository.save(inventory);
        }

        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setMethod(request.getPaymentMethod());
        payment.setAmount(savedOrder.getFinalAmount());
        payment.setStatus(resolvePaymentStatus(request.getPaymentMethod()));

        String checkoutUrl = null;
        if (request.getPaymentMethod() == PaymentMethod.PAYOS) {
            PayOsCheckoutService.PayOsCheckoutResult payOsCheckout =
                payOsCheckoutService.createCheckoutLink(savedOrder, orderItems, user, request);
            payment.setTransactionRef(payOsCheckout.paymentLinkId());
            checkoutUrl = payOsCheckout.checkoutUrl();
        }

        paymentRepository.save(payment);

        Shipment shipment = new Shipment();
        shipment.setOrder(savedOrder);
        shipment.setShippingStatus(ShipmentStatus.PENDING);
        shipmentRepository.save(shipment);

        Address address = new Address();
        address.setUser(user);
        address.setReceiverName(request.getReceiverName());
        address.setPhone(request.getPhone());
        address.setProvince(request.getProvince());
        address.setDistrict(request.getDistrict());
        address.setWard(request.getWard());
        address.setDetail(request.getDetail());
        address.setDefault(false);
        addressRepository.save(address);

        cart.setStatus(CartStatus.CHECKED_OUT);
        realtimeEventPublisher.publishCartUpdated(user.getEmail(), 0);

        return toOrderResponse(savedOrder, orderItems, request.getPaymentMethod(), checkoutUrl);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String email) {
        User user = getUserByEmail(email);
        return customerOrderRepository.findAllByUserIdWithItemsOrderByPlacedAtDesc(user.getId()).stream()
                .map(order -> {
                    PaymentMethod paymentMethod = paymentRepository
                            .findTopByOrderIdOrderByCreatedAtAsc(order.getId())
                            .map(Payment::getMethod)
                            .orElse(PaymentMethod.COD);
                        return toOrderResponse(order, order.getItems(), paymentMethod, null);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrderById(String email, Long orderId) {
        User user = getUserByEmail(email);
        CustomerOrder order = customerOrderRepository
                .findByIdAndUserIdWithItems(orderId, user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        PaymentMethod paymentMethod = paymentRepository
                .findTopByOrderIdOrderByCreatedAtAsc(order.getId())
                .map(Payment::getMethod)
                .orElse(PaymentMethod.COD);

        return toOrderResponse(order, order.getItems(), paymentMethod, null);
    }

    @Transactional
    public OrderResponse cancelMyOrder(String email, Long orderId) {
        User user = getUserByEmail(email);
        CustomerOrder order = customerOrderRepository
                .findByIdAndUserIdWithItems(orderId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Chỉ có thể hủy đơn hàng ở trạng thái chờ xử lý");
        }

        restoreInventory(order.getItems());
        order.setStatus(OrderStatus.CANCELLED);
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.FAILED);
        }

        shipmentRepository.findByOrderId(order.getId()).ifPresent(shipment -> shipment.setShippingStatus(ShipmentStatus.FAILED));

        PaymentMethod paymentMethod = paymentRepository
                .findTopByOrderIdOrderByCreatedAtAsc(order.getId())
                .map(Payment::getMethod)
                .orElse(PaymentMethod.COD);

        return toOrderResponse(order, order.getItems(), paymentMethod, null);
    }

    @Transactional
    public OrderResponse retryPayOsCheckout(String email, Long orderId) {
    User user = getUserByEmail(email);
    CustomerOrder order = customerOrderRepository
        .findByIdAndUserIdWithItems(orderId, user.getId())
        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

    Payment payment = paymentRepository
        .findTopByOrderIdOrderByCreatedAtDesc(order.getId())
        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bản ghi thanh toán cho đơn hàng"));

    if (payment.getMethod() != PaymentMethod.PAYOS) {
        throw new IllegalArgumentException("Đơn hàng này không sử dụng phương thức thanh toán PayOS");
    }

    if (order.getPaymentStatus() == PaymentStatus.PAID) {
        throw new IllegalArgumentException("Đơn hàng đã được thanh toán thành công");
    }

    Address latestAddress = addressRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId()).orElse(null);

    String buyerName = latestAddress != null
        ? latestAddress.getReceiverName()
        : user.getFullName();

    String buyerPhone = latestAddress != null
        ? latestAddress.getPhone()
        : user.getPhone();

    String buyerAddress = latestAddress != null
        ? String.join(
            ", ",
            latestAddress.getDetail(),
            latestAddress.getWard(),
            latestAddress.getDistrict(),
            latestAddress.getProvince())
        : "";

    PayOsCheckoutService.PayOsCheckoutResult payOsCheckout = payOsCheckoutService.createCheckoutLink(
        order,
        order.getItems(),
        buyerName,
        user.getEmail(),
        buyerPhone,
        buyerAddress);

    payment.setTransactionRef(payOsCheckout.paymentLinkId());
    payment.setStatus(PaymentStatus.PENDING);
    order.setPaymentStatus(PaymentStatus.PENDING);

    return toOrderResponse(order, order.getItems(), payment.getMethod(), payOsCheckout.checkoutUrl());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrdersForAdmin() {
        return customerOrderRepository.findAllWithItemsOrderByPlacedAtDesc().stream()
                .map(order -> {
                    PaymentMethod paymentMethod = paymentRepository
                            .findTopByOrderIdOrderByCreatedAtAsc(order.getId())
                            .map(Payment::getMethod)
                            .orElse(PaymentMethod.COD);
                    return toOrderResponse(order, order.getItems(), paymentMethod, null);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByIdForAdmin(Long orderId) {
        CustomerOrder order = customerOrderRepository
                .findByIdWithItems(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        PaymentMethod paymentMethod = paymentRepository
                .findTopByOrderIdOrderByCreatedAtAsc(order.getId())
                .map(Payment::getMethod)
                .orElse(PaymentMethod.COD);

                    return toOrderResponse(order, order.getItems(), paymentMethod, null);
    }

    @Transactional
    public OrderResponse updateOrderStatusForAdmin(Long orderId, OrderStatus newStatus) {
        CustomerOrder order = customerOrderRepository
                .findByIdWithItems(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        OrderStatus currentStatus = order.getStatus();
        if (!isValidTransition(currentStatus, newStatus)) {
            throw new IllegalArgumentException("Chuyển trạng thái không hợp lệ: " + currentStatus + " -> " + newStatus);
        }

        order.setStatus(newStatus);
        applyStatusSideEffects(order, newStatus);

        PaymentMethod paymentMethod = paymentRepository
                .findTopByOrderIdOrderByCreatedAtAsc(order.getId())
                .map(Payment::getMethod)
                .orElse(PaymentMethod.COD);

        return toOrderResponse(order, order.getItems(), paymentMethod, null);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
    }

    private PaymentStatus resolvePaymentStatus(PaymentMethod method) {
        return method == PaymentMethod.COD ? PaymentStatus.UNPAID : PaymentStatus.PENDING;
    }

    private void applyStatusSideEffects(CustomerOrder order, OrderStatus status) {
        Shipment shipment = shipmentRepository.findByOrderId(order.getId()).orElseGet(() -> {
            Shipment created = new Shipment();
            created.setOrder(order);
            created.setShippingStatus(ShipmentStatus.PENDING);
            return shipmentRepository.save(created);
        });

        switch (status) {
            case PROCESSING -> shipment.setShippingStatus(ShipmentStatus.PICKED_UP);
            case SHIPPED -> {
                shipment.setShippingStatus(ShipmentStatus.IN_TRANSIT);
                if (shipment.getShippedAt() == null) {
                    shipment.setShippedAt(LocalDateTime.now());
                }
            }
            case DELIVERED -> {
                shipment.setShippingStatus(ShipmentStatus.DELIVERED);
                if (shipment.getShippedAt() == null) {
                    shipment.setShippedAt(LocalDateTime.now());
                }
                shipment.setDeliveredAt(LocalDateTime.now());
                if (order.getPaymentStatus() == PaymentStatus.UNPAID) {
                    order.setPaymentStatus(PaymentStatus.PAID);
                }
            }
            case CANCELLED -> {
                restoreInventory(order.getItems());
                shipment.setShippingStatus(ShipmentStatus.FAILED);
                if (order.getPaymentStatus() != PaymentStatus.PAID) {
                    order.setPaymentStatus(PaymentStatus.FAILED);
                }
            }
            case RETURNED -> {
                shipment.setShippingStatus(ShipmentStatus.RETURNED);
                if (order.getPaymentStatus() == PaymentStatus.PAID) {
                    order.setPaymentStatus(PaymentStatus.REFUNDED);
                }
            }
            default -> {
                // No side effect required for PENDING / CONFIRMED.
            }
        }
    }

    private void restoreInventory(List<OrderItem> items) {
        for (OrderItem item : items) {
            Inventory inventory = inventoryRepository
                    .findByVariantId(item.getVariant().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tồn kho cho phiên bản sản phẩm"));
            inventory.setQuantityAvailable(inventory.getQuantityAvailable() + item.getQuantity());
            inventoryRepository.save(inventory);
        }
    }

    private boolean isValidTransition(OrderStatus from, OrderStatus to) {
        if (from == to) {
            return true;
        }

        return switch (from) {
            case PENDING -> to == OrderStatus.CONFIRMED || to == OrderStatus.CANCELLED;
            case CONFIRMED -> to == OrderStatus.PROCESSING || to == OrderStatus.CANCELLED;
            case PROCESSING -> to == OrderStatus.SHIPPED || to == OrderStatus.CANCELLED;
            case SHIPPED -> to == OrderStatus.DELIVERED || to == OrderStatus.RETURNED;
            case DELIVERED -> to == OrderStatus.RETURNED;
            case CANCELLED, RETURNED -> false;
        };
    }

    private String generateOrderCode() {
        Random random = new Random();
        String code;
        do {
            code = "LCG" + System.currentTimeMillis() + (100 + random.nextInt(900));
        } while (customerOrderRepository.existsByOrderCode(code));
        return code;
    }

    private OrderResponse toOrderResponse(
            CustomerOrder order,
            List<OrderItem> items,
            PaymentMethod paymentMethod,
            String checkoutUrl) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> new OrderItemResponse(
                        item.getVariant().getId(),
                        item.getSkuSnapshot(),
                        item.getProductSnapshotName(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getLineTotal()))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderCode(),
                checkoutUrl,
                order.getStatus(),
                order.getPaymentStatus(),
                paymentMethod,
                order.getTotalAmount(),
                order.getShippingFee(),
                order.getDiscountAmount(),
                order.getFinalAmount(),
                order.getPlacedAt(),
                itemResponses);
    }
}
