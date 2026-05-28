package com.financeapp.application.dto;

import com.financeapp.domain.enums.AccountType;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class AccountDTO {

    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    private String name;

    @NotNull(message = "Tipo é obrigatório")
    private AccountType type;

    private BigDecimal balance;

    private BigDecimal invoice;
}