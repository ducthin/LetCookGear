package com.ducthin.LetCookGear.controller;

import com.ducthin.LetCookGear.dto.ApiResponse;
import com.ducthin.LetCookGear.dto.CheckoutRequest;
import com.ducthin.LetCookGear.dto.OrderResponse;
import com.ducthin.LetCookGear.service.OrderService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            Principal principal, @Valid @RequestBody CheckoutRequest request) {
        OrderResponse data = orderService.checkout(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Đặt đơn thành công", data));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(Principal principal) {
        List<OrderResponse> data = orderService.getMyOrders(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng thành công", data));
    }

    @GetMapping("/me/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getMyOrderById(Principal principal, @PathVariable Long orderId) {
        OrderResponse data = orderService.getMyOrderById(principal.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Lấy đơn hàng thành công", data));
    }

    @PostMapping("/me/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelMyOrder(Principal principal, @PathVariable Long orderId) {
        OrderResponse data = orderService.cancelMyOrder(principal.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn hàng thành công", data));
    }

    @PostMapping("/me/{orderId}/payos/retry")
    public ResponseEntity<ApiResponse<OrderResponse>> retryPayOsCheckout(
            Principal principal,
            @PathVariable Long orderId) {
        OrderResponse data = orderService.retryPayOsCheckout(principal.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Tạo lại liên kết thanh toán PayOS thành công", data));
    }
}
