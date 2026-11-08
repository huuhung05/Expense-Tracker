package com.hung.Expense_Tracker.dto;

import com.hung.Expense_Tracker.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        BigDecimal amount,
        TransactionType type,
        Long categoryId,
        String categoryName,
        String description,
        LocalDate transactionDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
