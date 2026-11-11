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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

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
                .name("Dining Out")
                .type(TransactionType.EXPENSE)
                .user(testUser)
                .build();

        incomeCategory = Category.builder()
                .id(20L)
                .name("Freelance")
                .type(TransactionType.INCOME)
                .user(testUser)
                .build();
    }

    @Test
    @DisplayName("Should successfully create an expense transaction")
    void createTransaction_Success() {
        TransactionRequest request = new TransactionRequest(
                BigDecimal.valueOf(150.00),
                TransactionType.EXPENSE,
                10L,
                "Dinner with friends",
                LocalDate.now()
        );

        when(userRepository.findByEmail("hung@example.com")).thenReturn(Optional.of(testUser));
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(expenseCategory));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(100L);
            t.setCreatedAt(LocalDateTime.now());
            t.setUpdatedAt(LocalDateTime.now());
            return t;
        });

        TransactionResponse response = transactionService.createTransaction("hung@example.com", request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.amount()).isEqualTo(BigDecimal.valueOf(150.00));
        assertThat(response.type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(response.categoryName()).isEqualTo("Dining Out");
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when category type does not match transaction type")
    void createTransaction_TypeMismatch_ThrowsBadRequest() {
        TransactionRequest request = new TransactionRequest(
                BigDecimal.valueOf(200.00),
                TransactionType.EXPENSE,
                20L, // income category
                "Invalid assignment",
                LocalDate.now()
        );

        when(userRepository.findByEmail("hung@example.com")).thenReturn(Optional.of(testUser));
        when(categoryRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(incomeCategory));

        assertThatThrownBy(() -> transactionService.createTransaction("hung@example.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be used for an EXPENSE transaction");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should return paginated transaction responses")
    void getTransactions_PagedResult() {
        Pageable pageable = PageRequest.of(0, 10);
        Transaction transaction = Transaction.builder()
                .id(1L)
                .amount(BigDecimal.valueOf(50.0))
                .type(TransactionType.EXPENSE)
                .category(expenseCategory)
                .transactionDate(LocalDate.now())
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Page<Transaction> pagedTransactions = new PageImpl<>(List.of(transaction), pageable, 1);

        when(userRepository.findByEmail("hung@example.com")).thenReturn(Optional.of(testUser));
        when(transactionRepository.findFilteredTransactions(1L, null, null, null, null, pageable))
                .thenReturn(pagedTransactions);

        Page<TransactionResponse> result = transactionService.getTransactions(
                "hung@example.com", null, null, null, null, pageable
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).amount()).isEqualTo(BigDecimal.valueOf(50.0));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent transaction")
    void deleteTransaction_NotFound_ThrowsException() {
        when(userRepository.findByEmail("hung@example.com")).thenReturn(Optional.of(testUser));
        when(transactionRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.deleteTransaction("hung@example.com", 999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transactionRepository, never()).delete(any());
    }
}
