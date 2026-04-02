package com.ducthin.LetCookGear.controller;

import com.ducthin.LetCookGear.dto.ApiResponse;
import com.ducthin.LetCookGear.dto.OrderResponse;
import com.ducthin.LetCookGear.dto.OrderStatusUpdateRequest;
import com.ducthin.LetCookGear.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        List<OrderResponse> data = orderService.getAllOrdersForAdmin();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng quản trị thành công", data));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long orderId) {
        OrderResponse data = orderService.getOrderByIdForAdmin(orderId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đơn hàng quản trị thành công", data));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long orderId, @Valid @RequestBody OrderStatusUpdateRequest request) {
        OrderResponse data = orderService.updateOrderStatusForAdmin(orderId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đơn hàng thành công", data));
    }
}
