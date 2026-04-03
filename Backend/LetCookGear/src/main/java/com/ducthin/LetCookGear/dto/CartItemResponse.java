package com.ducthin.LetCookGear.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CartItemResponse {
    private Long itemId;
    private Long variantId;
    private String sku;
    private String variantName;
    private Long productId;
    private String productName;
    private String productSlug;
    private String imageUrl;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal lineTotal;
}
