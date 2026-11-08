package com.hung.Expense_Tracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BudgetRequest(
        @NotNull(message = "Category ID is required")
        Long categoryId,

        @NotNull(message = "Budget amount is required")
        @DecimalMin(value = "0.01", message = "Budget amount must be greater than 0")
        @Digits(integer = 13, fraction = 2, message = "Budget amount format is invalid")
        BigDecimal amount,

        @NotNull(message = "Month is required")
        @Min(value = 1, message = "Month must be between 1 and 12")
        @Max(value = 12, message = "Month must be between 1 and 12")
        Integer month,

        @NotNull(message = "Year is required")
        @Min(value = 2000, message = "Year must be 2000 or later")
        @Max(value = 2100, message = "Year must not exceed 2100")
        Integer year
) {
}
