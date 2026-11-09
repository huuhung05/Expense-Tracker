package com.hung.Expense_Tracker.controller;

import com.hung.Expense_Tracker.dto.AiAnalysisResponse;
import com.hung.Expense_Tracker.dto.MonthlySummaryResponse;
import com.hung.Expense_Tracker.service.AiAnalysisService;
import com.hung.Expense_Tracker.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final AiAnalysisService aiAnalysisService;

    @GetMapping("/monthly-summary")
    public ResponseEntity<MonthlySummaryResponse> getMonthlySummary(
            Authentication authentication,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        LocalDate now = LocalDate.now();
        int targetMonth = (month != null) ? month : now.getMonthValue();
        int targetYear = (year != null) ? year : now.getYear();

        return ResponseEntity.ok(
                reportService.getMonthlySummary(authentication.getName(), targetMonth, targetYear)
        );
    }

    @GetMapping("/ai-analysis")
    public ResponseEntity<AiAnalysisResponse> getAiSpendingAnalysis(
            Authentication authentication,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        LocalDate now = LocalDate.now();
        int targetMonth = (month != null) ? month : now.getMonthValue();
        int targetYear = (year != null) ? year : now.getYear();

        MonthlySummaryResponse summary = reportService.getMonthlySummary(authentication.getName(), targetMonth, targetYear);
        return ResponseEntity.ok(aiAnalysisService.analyzeSpending(summary));
    }
}
