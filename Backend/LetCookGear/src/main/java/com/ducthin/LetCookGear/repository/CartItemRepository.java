package com.ducthin.LetCookGear.repository;

import com.ducthin.LetCookGear.entity.CartItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndVariantId(Long cartId, Long variantId);

    Optional<CartItem> findByIdAndCartUserId(Long id, Long userId);
}
