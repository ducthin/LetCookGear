package com.ducthin.LetCookGear.repository;

import com.ducthin.LetCookGear.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {}
