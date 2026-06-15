package com.financeapp.application.dto;

import com.financeapp.domain.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionDTO {

    private Long id;

    @NotBlank(message = "Descrição é obrigatória")
    private String description;

    @NotNull(message = "Valor é obrigatório")
    @Positive(message = "Valor deve ser positivo")
    private BigDecimal amount;

    @NotNull(message = "Tipo é obrigatório")
    private TransactionType type;

    @NotNull(message = "Data é obrigatória")
    private LocalDate date;

    @NotNull(message = "Conta é obrigatória")
    private Long accountId;

    private Long categoryId;

    // Conta destino — obrigatório apenas em transferências entre contas próprias
    private Long destinationAccountId;

    // Campos de retorno
    private String accountName;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;

    private boolean future = false;
}