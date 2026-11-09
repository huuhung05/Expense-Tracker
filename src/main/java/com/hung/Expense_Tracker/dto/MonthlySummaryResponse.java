package com.hung.Expense_Tracker.dto;

import java.math.BigDecimal;
import java.util.List;

public record MonthlySummaryResponse(
        Integer month,
        Integer year,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netSavings,
        BigDecimal savingsRate,
        List<CategorySummary> expenseByCategory,
        List<CategorySummary> incomeByCategory
) {
}
