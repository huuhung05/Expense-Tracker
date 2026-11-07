package com.hung.Expense_Tracker.repository;

import com.hung.Expense_Tracker.entity.Transaction;
import com.hung.Expense_Tracker.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByUserId(Long userId, Pageable pageable);
    Page<Transaction> findByUserIdAndType(Long userId, TransactionType type, Pageable pageable);
    Page<Transaction> findByUserIdAndTransactionDateBetween(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable);
    Optional<Transaction> findByIdAndUserId(Long id, Long userId);
}
