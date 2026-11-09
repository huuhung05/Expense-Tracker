package com.hung.Expense_Tracker.dto;

import java.math.BigDecimal;

public record CategorySummary(
        Long categoryId,
        String categoryName,
        BigDecimal totalAmount,
        Long transactionCount
) {
}
