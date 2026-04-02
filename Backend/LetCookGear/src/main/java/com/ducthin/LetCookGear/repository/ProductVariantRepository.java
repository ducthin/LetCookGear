package com.ducthin.LetCookGear.repository;

import com.ducthin.LetCookGear.entity.ProductVariant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    Optional<ProductVariant> findBySku(String sku);

    @Query("select pv from ProductVariant pv join fetch pv.product p where pv.id = :id")
    Optional<ProductVariant> findByIdWithProduct(@Param("id") Long id);
}
