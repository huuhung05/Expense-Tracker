package com.hung.Expense_Tracker.service;

import com.hung.Expense_Tracker.dto.BudgetRequest;
import com.hung.Expense_Tracker.dto.BudgetResponse;
import com.hung.Expense_Tracker.entity.Budget;
import com.hung.Expense_Tracker.entity.Category;
import com.hung.Expense_Tracker.entity.TransactionType;
import com.hung.Expense_Tracker.entity.User;
import com.hung.Expense_Tracker.exception.BadRequestException;
import com.hung.Expense_Tracker.exception.DuplicateResourceException;
import com.hung.Expense_Tracker.repository.BudgetRepository;
import com.hung.Expense_Tracker.repository.CategoryRepository;
import com.hung.Expense_Tracker.repository.TransactionRepository;
import com.hung.Expense_Tracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BudgetService budgetService;

    private User testUser;
    private Category expenseCategory;
    private Category incomeCategory;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("hung@example.com")
                .fullName("Hung Nguyen")
                .build();

        expenseCategory = Category.builder()
                .id(10L)
                .name("Groceries")
                .type(TransactionType.EXPENSE)
                .user(testUser)
                .build();

        incomeCategory = Category.builder()
                .id(20L)
                .name("Salary")
                .type(TransactionType.INCOME)
                .user(testUser)
                .build();
    }

    @Test
    @DisplayName("Should successfully create a monthly budget")
    void createBudget_Success() {
        BudgetRequest request = new BudgetRequest(10L, BigDecimal.valueOf(1000.00), 11, 2026);

        when(userRepository.findByEmail("hung@example.com")).thenReturn(Optional.of(testUser));
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(expenseCategory));
        when(budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYear(1L, 10L, 11, 2026)).thenReturn(false);
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget b = invocation.getArgument(0);
            b.setId(50L);
            b.setCreatedAt(LocalDateTime.now());
            b.setUpdatedAt(LocalDateTime.now());
            return b;
        });
        when(transactionRepository.sumAmountByUserAndCategoryAndDateRange(
                eq(1L), eq(10L), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class))
        ).thenReturn(BigDecimal.valueOf(250.00));

        BudgetResponse response = budgetService.createBudget("hung@example.com", request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.limitAmount()).isEqualTo(BigDecimal.valueOf(1000.00));
        assertThat(response.spentAmount()).isEqualTo(BigDecimal.valueOf(250.00));
        assertThat(response.remainingAmount()).isEqualTo(BigDecimal.valueOf(750.00));
        assertThat(response.usagePercentage()).isEqualTo(BigDecimal.valueOf(25.00).setScale(2));
        assertThat(response.exceeded()).isFalse();
    }

    @Test
    @DisplayName("Should reject budget creation for INCOME category")
    void createBudget_IncomeCategory_ThrowsBadRequest() {
        BudgetRequest request = new BudgetRequest(20L, BigDecimal.valueOf(500.00), 11, 2026);

        when(userRepository.findByEmail("hung@example.com")).thenReturn(Optional.of(testUser));
        when(categoryRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(incomeCategory));

        assertThatThrownBy(() -> budgetService.createBudget("hung@example.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Budgets can only be set for EXPENSE categories");

        verify(budgetRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when budget already exists for month/year")
    void createBudget_Duplicate_ThrowsConflict() {
        BudgetRequest request = new BudgetRequest(10L, BigDecimal.valueOf(1000.00), 11, 2026);

        when(userRepository.findByEmail("hung@example.com")).thenReturn(Optional.of(testUser));
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(expenseCategory));
        when(budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYear(1L, 10L, 11, 2026)).thenReturn(true);

        assertThatThrownBy(() -> budgetService.createBudget("hung@example.com", request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Budget already exists");

        verify(budgetRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should accurately mark exceeded flag when spending exceeds budget limit")
    void getBudgets_OverspendingCalculation() {
        Budget budget = Budget.builder()
                .id(1L)
                .amount(BigDecimal.valueOf(500.00))
                .month(11)
                .year(2026)
                .category(expenseCategory)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("hung@example.com")).thenReturn(Optional.of(testUser));
        when(budgetRepository.findByUserIdAndMonthAndYear(1L, 11, 2026)).thenReturn(List.of(budget));
        when(transactionRepository.sumAmountByUserAndCategoryAndDateRange(
                eq(1L), eq(10L), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class))
        ).thenReturn(BigDecimal.valueOf(650.00)); // Spent 650 > Limit 500

        List<BudgetResponse> responses = budgetService.getBudgetsByMonthAndYear("hung@example.com", 11, 2026);

        assertThat(responses).hasSize(1);
        BudgetResponse res = responses.get(0);
        assertThat(res.spentAmount()).isEqualTo(BigDecimal.valueOf(650.00));
        assertThat(res.remainingAmount()).isEqualTo(BigDecimal.valueOf(-150.00));
        assertThat(res.exceeded()).isTrue();
    }
}
