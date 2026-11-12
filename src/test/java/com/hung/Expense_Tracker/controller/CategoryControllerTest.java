package com.hung.Expense_Tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hung.Expense_Tracker.dto.CategoryRequest;
import com.hung.Expense_Tracker.dto.CategoryResponse;
import com.hung.Expense_Tracker.entity.TransactionType;
import com.hung.Expense_Tracker.exception.GlobalExceptionHandler;
import com.hung.Expense_Tracker.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CategoryService categoryService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Authentication principal = new UsernamePasswordAuthenticationToken("hung@example.com", null, Collections.emptyList());

    @BeforeEach
    void setUp() {
        CategoryController categoryController = new CategoryController(categoryService);
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/categories - Should return list of user categories")
    void getCategories_Returns200() throws Exception {
        CategoryResponse response = new CategoryResponse(1L, "Utilities", TransactionType.EXPENSE, LocalDateTime.now());

        when(categoryService.getCategories(eq("hung@example.com"), any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/categories")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Utilities"))
                .andExpect(jsonPath("$[0].type").value("EXPENSE"));
    }

    @Test
    @DisplayName("POST /api/categories - Should return 201 Created on valid category")
    void createCategory_ValidPayload_Returns201() throws Exception {
        CategoryRequest request = new CategoryRequest("Salary", TransactionType.INCOME);
        CategoryResponse response = new CategoryResponse(2L, "Salary", TransactionType.INCOME, LocalDateTime.now());

        when(categoryService.createCategory(eq("hung@example.com"), any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/categories")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.name").value("Salary"));
    }

    @Test
    @DisplayName("DELETE /api/categories/{id} - Should return 204 No Content")
    void deleteCategory_Returns204() throws Exception {
        mockMvc.perform(delete("/api/categories/1")
                        .principal(principal))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteCategory("hung@example.com", 1L);
    }
}
