package com.ducthin.LetCookGear.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartAddItemRequest {

    @NotNull
    private Long variantId;

    @NotNull
    @Min(1)
    private Integer quantity;
}
