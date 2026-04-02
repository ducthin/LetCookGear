package com.ducthin.LetCookGear.repository;

import com.ducthin.LetCookGear.entity.CustomerOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    boolean existsByOrderCode(String orderCode);

    @Query("""
            select distinct o
            from CustomerOrder o
            left join fetch o.items i
            left join fetch i.variant v
            left join fetch v.product p
            where o.user.id = :userId
            order by o.placedAt desc
            """)
    List<CustomerOrder> findAllByUserIdWithItemsOrderByPlacedAtDesc(@Param("userId") Long userId);

    @Query("""
            select distinct o
            from CustomerOrder o
            left join fetch o.items i
            left join fetch i.variant v
            left join fetch v.product p
            where o.id = :orderId and o.user.id = :userId
            """)
    Optional<CustomerOrder> findByIdAndUserIdWithItems(@Param("orderId") Long orderId, @Param("userId") Long userId);

    @Query("""
            select distinct o
            from CustomerOrder o
            left join fetch o.items i
            left join fetch i.variant v
            left join fetch v.product p
            order by o.placedAt desc
            """)
    List<CustomerOrder> findAllWithItemsOrderByPlacedAtDesc();

    @Query("""
            select distinct o
            from CustomerOrder o
            left join fetch o.items i
            left join fetch i.variant v
            left join fetch v.product p
            where o.id = :orderId
            """)
    Optional<CustomerOrder> findByIdWithItems(@Param("orderId") Long orderId);
}
