package com.hung.Expense_Tracker.dto;

import com.hung.Expense_Tracker.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @Digits(integer = 13, fraction = 2, message = "Amount format is invalid")
        BigDecimal amount,

        @NotNull(message = "Transaction type (INCOME or EXPENSE) is required")
        TransactionType type,

        @NotNull(message = "Category ID is required")
        Long categoryId,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        @NotNull(message = "Transaction date is required")
        LocalDate transactionDate
) {
}
