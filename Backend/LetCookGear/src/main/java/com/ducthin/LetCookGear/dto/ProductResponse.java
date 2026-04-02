package com.ducthin.LetCookGear.dto;

import com.ducthin.LetCookGear.entity.enums.ProductStatus;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private String slug;
    private String shortDescription;
    private String description;
    private Integer warrantyMonths;
    private ProductStatus status;
    private Long categoryId;
    private String categoryName;
    private Long brandId;
    private String brandName;
    private List<ProductVariantSummaryResponse> variants;
}
