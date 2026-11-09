package com.hung.Expense_Tracker.service;

import com.hung.Expense_Tracker.dto.CategorySummary;
import com.hung.Expense_Tracker.dto.MonthlySummaryResponse;
import com.hung.Expense_Tracker.entity.TransactionType;
import com.hung.Expense_Tracker.entity.User;
import com.hung.Expense_Tracker.exception.BadRequestException;
import com.hung.Expense_Tracker.exception.ResourceNotFoundException;
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
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public MonthlySummaryResponse getMonthlySummary(String email, Integer month, Integer year) {
        validateMonthAndYear(month, year);
        User user = getUserByEmail(email);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        BigDecimal totalIncome = transactionRepository.sumAmountByUserAndTypeAndDateRange(
                user.getId(), TransactionType.INCOME, startDate, endDate
        );
        BigDecimal totalExpense = transactionRepository.sumAmountByUserAndTypeAndDateRange(
                user.getId(), TransactionType.EXPENSE, startDate, endDate
        );

        BigDecimal netSavings = totalIncome.subtract(totalExpense);
        BigDecimal savingsRate = BigDecimal.ZERO;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = netSavings
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalIncome, 2, RoundingMode.HALF_UP);
        }

        List<CategorySummary> expenseByCategory = transactionRepository.findCategorySummaries(
                user.getId(), TransactionType.EXPENSE, startDate, endDate
        );
        List<CategorySummary> incomeByCategory = transactionRepository.findCategorySummaries(
                user.getId(), TransactionType.INCOME, startDate, endDate
        );

        return new MonthlySummaryResponse(
                month,
                year,
                totalIncome,
                totalExpense,
                netSavings,
                savingsRate,
                expenseByCategory,
                incomeByCategory
        );
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
}
