package com.financeapp.application.dto;

import com.financeapp.domain.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDTO {

    private Long id;
    private String description;
    private BigDecimal amount;
    private TransactionType type;
    private LocalDate date;
    private LocalDateTime createdAt;

    private Long accountId;
    private String accountName;

    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;

    // Transferência entre contas próprias
    private Long destinationAccountId;
    private String destinationAccountName;
}