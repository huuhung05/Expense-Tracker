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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(String email, TransactionType type) {
        User user = getUserByEmail(email);
        List<Category> categories = (type != null)
                ? categoryRepository.findByUserIdAndType(user.getId(), type)
                : categoryRepository.findByUserId(user.getId());

        return categories.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(String email, CategoryRequest request) {
        User user = getUserByEmail(email);
        String trimmedName = request.name().trim();

        if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(user.getId(), trimmedName, request.type())) {
            throw new DuplicateResourceException("Category '" + trimmedName + "' already exists for type " + request.type());
        }

        Category category = Category.builder()
                .name(trimmedName)
                .type(request.type())
                .user(user)
                .build();

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(String email, Long categoryId, CategoryRequest request) {
        User user = getUserByEmail(email);
        Category category = categoryRepository.findByIdAndUserId(categoryId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        String trimmedName = request.name().trim();
        boolean nameOrTypeChanged = !category.getName().equalsIgnoreCase(trimmedName)
                || category.getType() != request.type();

        if (nameOrTypeChanged && categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(user.getId(), trimmedName, request.type())) {
            throw new DuplicateResourceException("Category '" + trimmedName + "' already exists for type " + request.type());
        }

        category.setName(trimmedName);
        category.setType(request.type());

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(String email, Long categoryId) {
        User user = getUserByEmail(email);
        Category category = categoryRepository.findByIdAndUserId(categoryId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        categoryRepository.delete(category);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getCreatedAt()
        );
    }
}
