package com.hung.Expense_Tracker.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BudgetResponse(
        Long id,
        Long categoryId,
        String categoryName,
        Integer month,
        Integer year,
        BigDecimal limitAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        BigDecimal usagePercentage,
        boolean exceeded,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
