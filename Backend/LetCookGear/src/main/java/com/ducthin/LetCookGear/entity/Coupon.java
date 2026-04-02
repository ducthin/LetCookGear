package com.ducthin.LetCookGear.entity;

import com.ducthin.LetCookGear.entity.enums.DiscountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "coupons")
public class Coupon extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType type;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal value;

    @Column(precision = 14, scale = 2)
    private BigDecimal maxDiscount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false)
    private Integer usageLimit = 0;

    @Column(nullable = false)
    private boolean isActive = true;
}
