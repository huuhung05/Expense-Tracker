package com.hung.Expense_Tracker.service;

import com.hung.Expense_Tracker.dto.CategoryRequest;
import com.hung.Expense_Tracker.dto.CategoryResponse;
import com.hung.Expense_Tracker.entity.Category;
import com.hung.Expense_Tracker.entity.TransactionType;
import com.hung.Expense_Tracker.entity.User;
import com.hung.Expense_Tracker.exception.DuplicateResourceException;
import com.hung.Expense_Tracker.exception.ResourceNotFoundException;
import com.hung.Expense_Tracker.repository.CategoryRepository;
import com.hung.Expense_Tracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CategoryService categoryService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("hung@example.com")
                .fullName("Hung Nguyen")
                .build();
    }

    @Test
    @DisplayName("Should return categories list filtered by type")
    void getCategories_FilteredByType() {
        Category cat = Category.builder()
                .id(10L)
                .name("Groceries")
                .type(TransactionType.EXPENSE)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("hung@example.com")).thenReturn(Optional.of(testUser));
        when(categoryRepository.findByUserIdAndType(1L, TransactionType.EXPENSE)).thenReturn(List.of(cat));

        List<CategoryResponse> result = categoryService.getCategories("hung@example.com", TransactionType.EXPENSE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Groceries");
        assertThat(result.get(0).type()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    @DisplayName("Should successfully create a category for user")
    void createCategory_Success() {
        CategoryRequest request = new CategoryRequest("Salary", TransactionType.INCOME);

        when(userRepository.findByEmail("hung@example.com")).thenReturn(Optional.of(testUser));
        when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(1L, "Salary", TransactionType.INCOME)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        CategoryResponse response = categoryService.createCategory("hung@example.com", request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Salary");
        assertThat(response.type()).isEqualTo(TransactionType.INCOME);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when category name exists for same type")
    void createCategory_DuplicateName_ThrowsException() {
        CategoryRequest request = new CategoryRequest("Groceries", TransactionType.EXPENSE);

        when(userRepository.findByEmail("hung@example.com")).thenReturn(Optional.of(testUser));
        when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(1L, "Groceries", TransactionType.EXPENSE)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory("hung@example.com", request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent category")
    void deleteCategory_NotFound_ThrowsException() {
        when(userRepository.findByEmail("hung@example.com")).thenReturn(Optional.of(testUser));
        when(categoryRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory("hung@example.com", 99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository, never()).delete(any());
    }
}
