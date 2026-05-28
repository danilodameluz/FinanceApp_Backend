package com.financeapp.application.dto;

import com.financeapp.domain.enums.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CategoryDTO {

    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    private String name;

    @NotNull(message = "Tipo é obrigatório")
    private CategoryType type;

    private String icon;
    private String color;
    private BigDecimal budget;
}