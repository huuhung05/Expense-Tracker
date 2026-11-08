package com.hung.Expense_Tracker.dto;

import com.hung.Expense_Tracker.entity.TransactionType;

import java.time.LocalDateTime;

public record CategoryResponse(
        Long id,
        String name,
        TransactionType type,
        LocalDateTime createdAt
) {
}
