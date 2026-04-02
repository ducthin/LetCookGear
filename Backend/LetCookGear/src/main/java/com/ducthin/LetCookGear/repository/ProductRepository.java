package com.ducthin.LetCookGear.repository;

import com.ducthin.LetCookGear.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
        select distinct p
        from Product p
        join fetch p.category
        join fetch p.brand
        left join fetch p.variants
        """)
    List<Product> findAllWithDetails();

    @Query("""
        select distinct p
        from Product p
        join fetch p.category
        join fetch p.brand
        left join fetch p.variants
        where p.id = :id
        """)
    Optional<Product> findByIdWithDetails(@Param("id") Long id);

    @Query("""
        select distinct p
        from Product p
        join fetch p.category
        join fetch p.brand
        left join fetch p.variants
        where p.slug = :slug
        """)
    Optional<Product> findBySlugWithDetails(@Param("slug") String slug);

    Optional<Product> findBySlug(String slug);
}
