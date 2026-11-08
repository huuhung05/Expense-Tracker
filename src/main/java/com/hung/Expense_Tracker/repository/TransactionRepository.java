package com.hung.Expense_Tracker.repository;

import com.hung.Expense_Tracker.entity.Transaction;
import com.hung.Expense_Tracker.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @EntityGraph(attributePaths = "category")
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.user.id = :userId
              AND (:type IS NULL OR t.type = :type)
              AND (:categoryId IS NULL OR t.category.id = :categoryId)
              AND (:startDate IS NULL OR t.transactionDate >= :startDate)
              AND (:endDate IS NULL OR t.transactionDate <= :endDate)
            """)
    Page<Transaction> findFilteredTransactions(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "category")
    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.category.id = :categoryId
              AND t.type = :type
              AND t.transactionDate BETWEEN :startDate AND :endDate
            """)
    BigDecimal sumAmountByUserAndCategoryAndDateRange(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
