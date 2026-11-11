package com.hung.Expense_Tracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hung.Expense_Tracker.dto.AiAnalysisResponse;
import com.hung.Expense_Tracker.dto.CategorySummary;
import com.hung.Expense_Tracker.dto.MonthlySummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiAnalysisServiceTest {

    private AiAnalysisService aiAnalysisService;

    @BeforeEach
    void setUp() {
        aiAnalysisService = new AiAnalysisService(new ObjectMapper());
    }

    @Test
    @DisplayName("Should assign EXCELLENT score when savings rate exceeds 30%")
    void generateRuleBasedAnalysis_ExcellentSavingsRate() {
        MonthlySummaryResponse summary = new MonthlySummaryResponse(
                11,
                2026,
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(6000),
                BigDecimal.valueOf(4000),
                BigDecimal.valueOf(40.0),
                List.of(new CategorySummary(1L, "Food", BigDecimal.valueOf(3000), 10L)),
                List.of(new CategorySummary(2L, "Salary", BigDecimal.valueOf(10000), 1L))
        );

        AiAnalysisResponse response = aiAnalysisService.generateRuleBasedAnalysis(summary);

        assertThat(response).isNotNull();
        assertThat(response.financialHealthScore()).isEqualTo("EXCELLENT");
        assertThat(response.savingsRate()).isEqualTo(BigDecimal.valueOf(40.0));
        assertThat(response.strengths()).isNotEmpty();
        assertThat(response.aiGenerated()).isFalse();
    }

    @Test
    @DisplayName("Should assign POOR score when spending exceeds income (net deficit)")
    void generateRuleBasedAnalysis_DeficitSpending() {
        MonthlySummaryResponse summary = new MonthlySummaryResponse(
                11,
                2026,
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(7000),
                BigDecimal.valueOf(-2000),
                BigDecimal.valueOf(-40.0),
                List.of(new CategorySummary(1L, "Shopping", BigDecimal.valueOf(5000), 5L)),
                List.of()
        );

        AiAnalysisResponse response = aiAnalysisService.generateRuleBasedAnalysis(summary);

        assertThat(response).isNotNull();
        assertThat(response.financialHealthScore()).isEqualTo("POOR");
        assertThat(response.areasToImprove()).isNotEmpty();
        assertThat(response.areasToImprove().get(0)).contains("deficit");
    }

    @Test
    @DisplayName("Should return NEUTRAL score with clean slate message when no transactions exist")
    void generateRuleBasedAnalysis_EmptyTransactions() {
        MonthlySummaryResponse summary = new MonthlySummaryResponse(
                11,
                2026,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                List.of()
        );

        AiAnalysisResponse response = aiAnalysisService.generateRuleBasedAnalysis(summary);

        assertThat(response).isNotNull();
        assertThat(response.financialHealthScore()).isEqualTo("NEUTRAL");
        assertThat(response.summary()).contains("No transactions recorded");
    }
}
