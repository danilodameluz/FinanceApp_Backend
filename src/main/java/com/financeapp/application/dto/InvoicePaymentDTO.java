package com.financeapp.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class InvoicePaymentDTO {

    @NotNull(message = "Conta de débito é obrigatória")
    private Long debitAccountId;

    @Positive(message = "Valor deve ser positivo")
    private BigDecimal amount; // null = pagar fatura total
}