package com.hung.Expense_Tracker.controller;

import com.hung.Expense_Tracker.dto.BudgetRequest;
import com.hung.Expense_Tracker.dto.BudgetResponse;
import com.hung.Expense_Tracker.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getBudgets(
            Authentication authentication,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        LocalDate now = LocalDate.now();
        int targetMonth = (month != null) ? month : now.getMonthValue();
        int targetYear = (year != null) ? year : now.getYear();

        return ResponseEntity.ok(
                budgetService.getBudgetsByMonthAndYear(authentication.getName(), targetMonth, targetYear)
        );
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
            Authentication authentication,
            @Valid @RequestBody BudgetRequest request
    ) {
        BudgetResponse created = budgetService.createBudget(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> updateBudget(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request
    ) {
        return ResponseEntity.ok(budgetService.updateBudget(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(
            Authentication authentication,
            @PathVariable Long id
    ) {
        budgetService.deleteBudget(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
