package com.ducthin.LetCookGear.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BrandResponse {
    private Long id;
    private String name;
    private String slug;
    private String country;
    private boolean isActive;
}
