package com.ducthin.LetCookGear.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CartResponse {
    private Long cartId;
    private Integer totalItems;
    private BigDecimal subtotal;
    private List<CartItemResponse> items;
}
