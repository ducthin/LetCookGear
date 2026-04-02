package com.ducthin.LetCookGear.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    private Long parentId;

    private Boolean isActive;
}
