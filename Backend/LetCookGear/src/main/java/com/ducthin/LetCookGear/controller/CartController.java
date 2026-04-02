package com.ducthin.LetCookGear.controller;

import com.ducthin.LetCookGear.dto.ApiResponse;
import com.ducthin.LetCookGear.dto.CartAddItemRequest;
import com.ducthin.LetCookGear.dto.CartResponse;
import com.ducthin.LetCookGear.dto.CartUpdateItemRequest;
import com.ducthin.LetCookGear.service.CartService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getMyCart(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Lấy giỏ hàng thành công", cartService.getMyCart(principal.getName())));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            Principal principal, @Valid @RequestBody CartAddItemRequest request) {
        CartResponse data = cartService.addItem(principal.getName(), request.getVariantId(), request.getQuantity());
        return ResponseEntity.ok(ApiResponse.success("Them sản phẩm vào giỏ hàng thành công", data));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            Principal principal, @PathVariable Long itemId, @Valid @RequestBody CartUpdateItemRequest request) {
        CartResponse data = cartService.updateItemQuantity(principal.getName(), itemId, request.getQuantity());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật sản phẩm trong giỏ hàng thành công", data));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(Principal principal, @PathVariable Long itemId) {
        CartResponse data = cartService.removeItem(principal.getName(), itemId);
        return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm khỏi giỏ hàng thành công", data));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<CartResponse>> clearCart(Principal principal) {
        CartResponse data = cartService.clearMyCart(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Xóa giỏ hàng thành công", data));
    }
}
