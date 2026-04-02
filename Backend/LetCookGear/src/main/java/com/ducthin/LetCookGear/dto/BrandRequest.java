package com.ducthin.LetCookGear.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrandRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    private String country;

    private Boolean isActive;
}
