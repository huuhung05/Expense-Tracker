package com.hung.Expense_Tracker.controller;

import com.hung.Expense_Tracker.dto.CategoryRequest;
import com.hung.Expense_Tracker.dto.CategoryResponse;
import com.hung.Expense_Tracker.entity.TransactionType;
import com.hung.Expense_Tracker.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories(
            Authentication authentication,
            @RequestParam(required = false) TransactionType type
    ) {
        return ResponseEntity.ok(categoryService.getCategories(authentication.getName(), type));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            Authentication authentication,
            @Valid @RequestBody CategoryRequest request
    ) {
        CategoryResponse created = categoryService.createCategory(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request
    ) {
        return ResponseEntity.ok(categoryService.updateCategory(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            Authentication authentication,
            @PathVariable Long id
    ) {
        categoryService.deleteCategory(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
