package com.ducthin.LetCookGear.dto;

import com.ducthin.LetCookGear.entity.enums.ProductStatus;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductVariantSummaryResponse {
    private Long id;
    private String sku;
    private String variantName;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private String cpuModel;
    private Integer ramGb;
    private Integer storageGb;
    private String gpuModel;
    private Integer refreshRateHz;
    private String panelType;
    private String connectionType;
    private String switchType;
    private BigDecimal sizeInch;
    private ProductStatus status;
}
