package com.ducthin.LetCookGear.dto;

import com.ducthin.LetCookGear.entity.enums.ProductStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequest {

    @NotNull
    private Long categoryId;

    @NotNull
    private Long brandId;

    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    private String shortDescription;

    private String description;

    private Integer warrantyMonths;

    private ProductStatus status;
}
