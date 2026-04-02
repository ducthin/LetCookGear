package com.ducthin.LetCookGear.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartUpdateItemRequest {

    @NotNull
    @Min(0)
    private Integer quantity;
}
