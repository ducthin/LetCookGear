package com.ducthin.LetCookGear.service;

import com.ducthin.LetCookGear.dto.CartItemResponse;
import com.ducthin.LetCookGear.dto.CartResponse;
import com.ducthin.LetCookGear.entity.Cart;
import com.ducthin.LetCookGear.entity.CartItem;
import com.ducthin.LetCookGear.entity.Inventory;
import com.ducthin.LetCookGear.entity.ProductVariant;
import com.ducthin.LetCookGear.entity.User;
import com.ducthin.LetCookGear.entity.enums.CartStatus;
import com.ducthin.LetCookGear.repository.CartItemRepository;
import com.ducthin.LetCookGear.repository.CartRepository;
import com.ducthin.LetCookGear.repository.InventoryRepository;
import com.ducthin.LetCookGear.repository.ProductVariantRepository;
import com.ducthin.LetCookGear.repository.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public CartResponse getMyCart(String email) {
        User user = getUserByEmail(email);
        Cart cart = getOrCreateActiveCart(user);
        return toCartResponse(cart);
    }

    @Transactional
    public CartResponse addItem(String email, Long variantId, Integer quantityToAdd) {
        if (quantityToAdd == null || quantityToAdd < 1) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
        }

        User user = getUserByEmail(email);
        Cart cart = getOrCreateActiveCart(user);
        ProductVariant variant = productVariantRepository
                .findByIdWithProduct(variantId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên bản sản phẩm"));

        CartItem item = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variantId).orElseGet(() -> {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setVariant(variant);
            newItem.setQuantity(0);
            newItem.setUnitPrice(variant.getPrice());
            return newItem;
        });

        int newQuantity = item.getQuantity() + quantityToAdd;
        validateInventory(variantId, newQuantity);

        item.setQuantity(newQuantity);
        item.setUnitPrice(variant.getPrice());
        cartItemRepository.save(item);

        return toCartResponse(getActiveCartOrThrow(user.getId()));
    }

    @Transactional
    public CartResponse updateItemQuantity(String email, Long itemId, Integer quantity) {
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Số lượng không được âm");
        }

        User user = getUserByEmail(email);
        CartItem item = cartItemRepository
                .findByIdAndCartUserId(itemId, user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong giỏ hàng"));

        if (quantity == 0) {
            cartItemRepository.delete(item);
        } else {
            validateInventory(item.getVariant().getId(), quantity);
            item.setQuantity(quantity);
            item.setUnitPrice(item.getVariant().getPrice());
            cartItemRepository.save(item);
        }

        return toCartResponse(getOrCreateActiveCart(user));
    }

    @Transactional
    public CartResponse removeItem(String email, Long itemId) {
        User user = getUserByEmail(email);
        CartItem item = cartItemRepository
                .findByIdAndCartUserId(itemId, user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong giỏ hàng"));
        cartItemRepository.delete(item);

        return toCartResponse(getOrCreateActiveCart(user));
    }

    @Transactional
    public CartResponse clearMyCart(String email) {
        User user = getUserByEmail(email);
        Cart cart = getOrCreateActiveCart(user);
        List<CartItem> existingItems = new ArrayList<>(cart.getItems());
        cartItemRepository.deleteAll(existingItems);
        return toCartResponse(getActiveCartOrThrow(user.getId()));
    }

    @Transactional(readOnly = true)
    public Cart getActiveCartOrThrow(Long userId) {
        return cartRepository
                .findByUserIdAndStatusWithItems(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giỏ hàng đang hoạt động"));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
    }

    private Cart getOrCreateActiveCart(User user) {
        return cartRepository.findByUserIdAndStatusWithItems(user.getId(), CartStatus.ACTIVE).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUser(user);
            cart.setStatus(CartStatus.ACTIVE);
            return cartRepository.save(cart);
        });
    }

    private void validateInventory(Long variantId, Integer requiredQuantity) {
        Inventory inventory = inventoryRepository
                .findByVariantId(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tồn kho cho phiên bản sản phẩm"));

        if (inventory.getQuantityAvailable() < requiredQuantity) {
            throw new IllegalArgumentException("Không đủ tồn kho cho phiên bản đã chọn");
        }
    }

    private CartResponse toCartResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        int totalItems = items.stream().mapToInt(CartItemResponse::getQuantity).sum();
        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(cart.getId(), totalItems, subtotal, items);
    }

    private CartItemResponse toItemResponse(CartItem item) {
        BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemResponse(
                item.getId(),
                item.getVariant().getId(),
                item.getVariant().getSku(),
                item.getVariant().getVariantName(),
                item.getVariant().getProduct().getId(),
                item.getVariant().getProduct().getName(),
                item.getVariant().getProduct().getSlug(),
                item.getUnitPrice(),
                item.getQuantity(),
                lineTotal);
    }
}
