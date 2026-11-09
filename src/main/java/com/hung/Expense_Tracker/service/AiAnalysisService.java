package com.hung.Expense_Tracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hung.Expense_Tracker.dto.AiAnalysisResponse;
import com.hung.Expense_Tracker.dto.CategorySummary;
import com.hung.Expense_Tracker.dto.MonthlySummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${ai.gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    public AiAnalysisService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofSeconds(5));
        factory.setReadTimeout(java.time.Duration.ofSeconds(15));
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    public AiAnalysisResponse analyzeSpending(MonthlySummaryResponse summary) {
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            try {
                return callGeminiApi(summary);
            } catch (Exception ex) {
                log.warn("Gemini API call failed, falling back to rule-based financial engine: {}", ex.getMessage());
            }
        }
        return generateRuleBasedAnalysis(summary);
    }

    private AiAnalysisResponse callGeminiApi(MonthlySummaryResponse summary) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + geminiModel + ":generateContent?key=" + geminiApiKey;

        String prompt = buildPrompt(summary);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        String responseBody = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        JsonNode rootNode = objectMapper.readTree(responseBody);
        String text = rootNode.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();

        return parseGeminiResponse(text, summary);
    }

    private String buildPrompt(MonthlySummaryResponse summary) {
        return """
                You are a certified personal finance advisor. Analyze the following monthly spending summary and respond strictly in JSON format matching this schema:
                {
                  "financialHealthScore": "GOOD", // Options: EXCELLENT, GOOD, FAIR, POOR
                  "summary": "Short 2-3 sentence overview of this month's financial status.",
                  "strengths": ["Strength 1", "Strength 2"],
                  "areasToImprove": ["Area 1", "Area 2"],
                  "actionableTips": ["Specific tip 1", "Specific tip 2"]
                }

                Financial Data:
                - Month/Year: %d/%d
                - Total Income: %s
                - Total Expense: %s
                - Net Savings: %s
                - Savings Rate: %s%%
                - Top Expense Categories: %s
                """.formatted(
                summary.month(),
                summary.year(),
                summary.totalIncome(),
                summary.totalExpense(),
                summary.netSavings(),
                summary.savingsRate(),
                summary.expenseByCategory().stream()
                        .map(c -> c.categoryName() + ": " + c.totalAmount())
                        .toList()
        );
    }

    private AiAnalysisResponse parseGeminiResponse(String text, MonthlySummaryResponse summary) {
        try {
            String cleanedJson = text.replaceAll("```json", "").replaceAll("```", "").trim();
            JsonNode node = objectMapper.readTree(cleanedJson);

            List<String> strengths = new ArrayList<>();
            node.path("strengths").forEach(item -> strengths.add(item.asText()));

            List<String> areasToImprove = new ArrayList<>();
            node.path("areasToImprove").forEach(item -> areasToImprove.add(item.asText()));

            List<String> tips = new ArrayList<>();
            node.path("actionableTips").forEach(item -> tips.add(item.asText()));

            return new AiAnalysisResponse(
                    summary.month(),
                    summary.year(),
                    summary.totalIncome(),
                    summary.totalExpense(),
                    summary.savingsRate(),
                    node.path("financialHealthScore").asText("GOOD"),
                    node.path("summary").asText("Spending analysis completed."),
                    strengths,
                    areasToImprove,
                    tips,
                    true
            );
        } catch (Exception e) {
            log.warn("Failed to parse Gemini JSON response, falling back to rule engine: {}", e.getMessage());
            return generateRuleBasedAnalysis(summary);
        }
    }

    public AiAnalysisResponse generateRuleBasedAnalysis(MonthlySummaryResponse summary) {
        BigDecimal savingsRate = summary.savingsRate();
        BigDecimal totalExpense = summary.totalExpense();
        BigDecimal totalIncome = summary.totalIncome();

        String healthScore;
        String overview;
        List<String> strengths = new ArrayList<>();
        List<String> areasToImprove = new ArrayList<>();
        List<String> tips = new ArrayList<>();

        if (totalIncome.compareTo(BigDecimal.ZERO) == 0 && totalExpense.compareTo(BigDecimal.ZERO) == 0) {
            return new AiAnalysisResponse(
                    summary.month(),
                    summary.year(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    "NEUTRAL",
                    "No transactions recorded for this month yet. Start tracking your daily expenses to receive insights.",
                    List.of("Clean slate for budget planning"),
                    List.of("No expense records found"),
                    List.of("Log your daily expenses and income regularly."),
                    false
            );
        }

        if (savingsRate.compareTo(BigDecimal.valueOf(30)) >= 0) {
            healthScore = "EXCELLENT";
            overview = "Outstanding financial discipline! You saved over " + savingsRate + "% of your income this month.";
            strengths.add("High savings rate exceeding the recommended 20% benchmark.");
            strengths.add("Net positive cash flow of " + summary.netSavings() + ".");
            tips.add("Consider investing your surplus savings into diversified index funds or emergency reserves.");
        } else if (savingsRate.compareTo(BigDecimal.valueOf(10)) >= 0) {
            healthScore = "GOOD";
            overview = "Healthy financial footing. You maintained a positive savings rate of " + savingsRate + "%.";
            strengths.add("Maintained positive net cash flow.");
            areasToImprove.add("Aim to reach a 20% savings target by cutting discretionary expenses.");
            tips.add("Review variable recurring expenses to find optimization opportunities.");
        } else if (savingsRate.compareTo(BigDecimal.ZERO) >= 0) {
            healthScore = "FAIR";
            overview = "Tight budget margin. Savings rate is only " + savingsRate + "%, leaving little room for unexpected events.";
            areasToImprove.add("Very thin savings buffer. An unexpected expense could cause a deficit.");
            tips.add("Establish a strict 50/30/20 budget framework.");
        } else {
            healthScore = "POOR";
            overview = "Spending exceeded total income this month. Net deficit of " + summary.netSavings().abs() + ".";
            areasToImprove.add("Operating at a financial deficit (living beyond current monthly earnings).");
            tips.add("Freeze non-essential purchases immediately and review your top spending categories.");
        }

        if (!summary.expenseByCategory().isEmpty()) {
            CategorySummary topExpense = summary.expenseByCategory().get(0);
            if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal categoryShare = topExpense.totalAmount()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalIncome, 1, RoundingMode.HALF_UP);
                if (categoryShare.compareTo(BigDecimal.valueOf(35)) > 0) {
                    areasToImprove.add("Category '" + topExpense.categoryName() + "' consumed "
                            + categoryShare + "% of total income (" + topExpense.totalAmount() + ").");
                    tips.add("Set a category budget cap on '" + topExpense.categoryName() + "' to curb high outflow.");
                }
            }
        }

        return new AiAnalysisResponse(
                summary.month(),
                summary.year(),
                summary.totalIncome(),
                summary.totalExpense(),
                summary.savingsRate(),
                healthScore,
                overview,
                strengths,
                areasToImprove,
                tips,
                false
        );
    }
}
