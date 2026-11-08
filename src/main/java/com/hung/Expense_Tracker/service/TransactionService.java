package com.hung.Expense_Tracker.service;

import com.hung.Expense_Tracker.dto.TransactionRequest;
import com.hung.Expense_Tracker.dto.TransactionResponse;
import com.hung.Expense_Tracker.entity.Category;
import com.hung.Expense_Tracker.entity.Transaction;
import com.hung.Expense_Tracker.entity.TransactionType;
import com.hung.Expense_Tracker.entity.User;
import com.hung.Expense_Tracker.exception.BadRequestException;
import com.hung.Expense_Tracker.exception.ResourceNotFoundException;
import com.hung.Expense_Tracker.repository.CategoryRepository;
import com.hung.Expense_Tracker.repository.TransactionRepository;
import com.hung.Expense_Tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(
            String email,
            TransactionType type,
            Long categoryId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("startDate cannot be after endDate");
        }

        User user = getUserByEmail(email);
        return transactionRepository.findFilteredTransactions(
                        user.getId(), type, categoryId, startDate, endDate, pageable
                )
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(String email, Long transactionId) {
        User user = getUserByEmail(email);
        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse createTransaction(String email, TransactionRequest request) {
        User user = getUserByEmail(email);
        Category category = getValidatedCategory(user.getId(), request.categoryId(), request.type());

        Transaction transaction = Transaction.builder()
                .amount(request.amount())
                .type(request.type())
                .category(category)
                .description(request.description() != null ? request.description().trim() : null)
                .transactionDate(request.transactionDate())
                .user(user)
                .build();

        return toResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse updateTransaction(String email, Long transactionId, TransactionRequest request) {
        User user = getUserByEmail(email);
        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        Category category = getValidatedCategory(user.getId(), request.categoryId(), request.type());

        transaction.setAmount(request.amount());
        transaction.setType(request.type());
        transaction.setCategory(category);
        transaction.setDescription(request.description() != null ? request.description().trim() : null);
        transaction.setTransactionDate(request.transactionDate());

        return toResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public void deleteTransaction(String email, Long transactionId) {
        User user = getUserByEmail(email);
        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        transactionRepository.delete(transaction);
    }

    private Category getValidatedCategory(Long userId, Long categoryId, TransactionType transactionType) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        if (category.getType() != transactionType) {
            throw new BadRequestException(
                    "Category '" + category.getName() + "' is of type " + category.getType()
                            + " and cannot be used for an " + transactionType + " transaction"
            );
        }
        return category;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getCategory().getId(),
                transaction.getCategory().getName(),
                transaction.getDescription(),
                transaction.getTransactionDate(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
}
