package com.hung.Expense_Tracker.dto;

import com.hung.Expense_Tracker.entity.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(max = 50, message = "Category name must not exceed 50 characters")
        String name,

        @NotNull(message = "Transaction type (INCOME or EXPENSE) is required")
        TransactionType type
) {
}
