package com.ducthin.LetCookGear.repository;

import com.ducthin.LetCookGear.entity.Inventory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    boolean existsByVariantId(Long variantId);

    Optional<Inventory> findByVariantId(Long variantId);
}
