package com.hung.Expense_Tracker.dto;

import java.math.BigDecimal;
import java.util.List;

public record AiAnalysisResponse(
        Integer month,
        Integer year,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal savingsRate,
        String financialHealthScore,
        String summary,
        List<String> strengths,
        List<String> areasToImprove,
        List<String> actionableTips,
        boolean aiGenerated
) {
}
