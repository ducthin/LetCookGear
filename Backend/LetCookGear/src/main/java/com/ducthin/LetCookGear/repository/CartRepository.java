package com.ducthin.LetCookGear.repository;

import com.ducthin.LetCookGear.entity.Cart;
import com.ducthin.LetCookGear.entity.enums.CartStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserIdAndStatus(Long userId, CartStatus status);

    @Query("""
            select distinct c
            from Cart c
            left join fetch c.items i
            left join fetch i.variant v
            left join fetch v.product p
            where c.user.id = :userId and c.status = :status
            """)
    Optional<Cart> findByUserIdAndStatusWithItems(@Param("userId") Long userId, @Param("status") CartStatus status);
}
