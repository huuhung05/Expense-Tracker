package com.hung.Expense_Tracker.repository;

import com.hung.Expense_Tracker.entity.Budget;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    @EntityGraph(attributePaths = "category")
    List<Budget> findByUserIdAndMonthAndYear(Long userId, Integer month, Integer year);

    @EntityGraph(attributePaths = "category")
    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndCategoryIdAndMonthAndYear(Long userId, Long categoryId, Integer month, Integer year);
}
