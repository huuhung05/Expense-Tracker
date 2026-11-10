package com.hung.Expense_Tracker.service;

import com.hung.Expense_Tracker.dto.BudgetRequest;
import com.hung.Expense_Tracker.dto.BudgetResponse;
import com.hung.Expense_Tracker.entity.Budget;
import com.hung.Expense_Tracker.entity.Category;
import com.hung.Expense_Tracker.entity.TransactionType;
import com.hung.Expense_Tracker.entity.User;
import com.hung.Expense_Tracker.exception.BadRequestException;
import com.hung.Expense_Tracker.exception.DuplicateResourceException;
import com.hung.Expense_Tracker.exception.ResourceNotFoundException;
import com.hung.Expense_Tracker.repository.BudgetRepository;
import com.hung.Expense_Tracker.repository.CategoryRepository;
import com.hung.Expense_Tracker.repository.TransactionRepository;
import com.hung.Expense_Tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsByMonthAndYear(String email, Integer month, Integer year) {
        validateMonthAndYear(month, year);
        User user = getUserByEmail(email);

        return budgetRepository.findByUserIdAndMonthAndYear(user.getId(), month, year)
                .stream()
                .map(budget -> toResponse(budget, user.getId()))
                .toList();
    }

    @Transactional
    public BudgetResponse createBudget(String email, BudgetRequest request) {
        User user = getUserByEmail(email);
        Category category = getValidatedExpenseCategory(user.getId(), request.categoryId());

        if (budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYear(
                user.getId(), category.getId(), request.month(), request.year())) {
            throw new DuplicateResourceException(
                    "Budget already exists for category '" + category.getName()
                            + "' in " + request.month() + "/" + request.year()
            );
        }

        Budget budget = Budget.builder()
                .amount(request.amount())
                .month(request.month())
                .year(request.year())
                .category(category)
                .user(user)
                .build();

        return toResponse(budgetRepository.save(budget), user.getId());
    }

    @Transactional
    public BudgetResponse updateBudget(String email, Long budgetId, BudgetRequest request) {
        User user = getUserByEmail(email);
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + budgetId));

        Category category = getValidatedExpenseCategory(user.getId(), request.categoryId());

        boolean targetChanged = !budget.getCategory().getId().equals(category.getId())
                || !budget.getMonth().equals(request.month())
                || !budget.getYear().equals(request.year());

        if (targetChanged && budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYear(
                user.getId(), category.getId(), request.month(), request.year())) {
            throw new DuplicateResourceException(
                    "Budget already exists for category '" + category.getName()
                            + "' in " + request.month() + "/" + request.year()
            );
        }

        budget.setCategory(category);
        budget.setAmount(request.amount());
        budget.setMonth(request.month());
        budget.setYear(request.year());

        return toResponse(budgetRepository.save(budget), user.getId());
    }

    @Transactional
    public void deleteBudget(String email, Long budgetId) {
        User user = getUserByEmail(email);
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + budgetId));

        budgetRepository.delete(budget);
    }

    private Category getValidatedExpenseCategory(Long userId, Long categoryId) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        if (category.getType() != TransactionType.EXPENSE) {
            throw new BadRequestException("Budgets can only be set for EXPENSE categories");
        }
        return category;
    }

    private void validateMonthAndYear(Integer month, Integer year) {
        if (month == null || month < 1 || month > 12) {
            throw new BadRequestException("Month must be between 1 and 12");
        }
        if (year == null || year < 2000 || year > 2100) {
            throw new BadRequestException("Year must be between 2000 and 2100");
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private BudgetResponse toResponse(Budget budget, Long userId) {
        YearMonth yearMonth = YearMonth.of(budget.getYear(), budget.getMonth());
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        BigDecimal spentAmount = transactionRepository.sumAmountByUserAndCategoryAndDateRange(
                userId,
                budget.getCategory().getId(),
                TransactionType.EXPENSE,
                startDate,
                endDate
        );

        BigDecimal limitAmount = budget.getAmount();
        BigDecimal remainingAmount = limitAmount.subtract(spentAmount);

        // Guard against division by zero when limitAmount = 0
        BigDecimal usagePercentage = BigDecimal.ZERO;
        if (limitAmount.compareTo(BigDecimal.ZERO) > 0) {
            usagePercentage = spentAmount
                    .multiply(BigDecimal.valueOf(100))
                    .divide(limitAmount, 2, RoundingMode.HALF_UP);
        }
        boolean exceeded = spentAmount.compareTo(limitAmount) > 0;

        return new BudgetResponse(
                budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                budget.getMonth(),
                budget.getYear(),
                limitAmount,
                spentAmount,
                remainingAmount,
                usagePercentage,
                exceeded,
                budget.getCreatedAt(),
                budget.getUpdatedAt()
        );
    }
}
