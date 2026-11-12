package com.hung.Expense_Tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hung.Expense_Tracker.dto.AuthResponse;
import com.hung.Expense_Tracker.dto.LoginRequest;
import com.hung.Expense_Tracker.dto.RegisterRequest;
import com.hung.Expense_Tracker.dto.UserResponse;
import com.hung.Expense_Tracker.exception.GlobalExceptionHandler;
import com.hung.Expense_Tracker.service.AuthService;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private Bucket loginLimiter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(authService, loginLimiter);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 201 Created on valid input")
    void register_ValidPayload_Returns201() throws Exception {
        RegisterRequest request = new RegisterRequest("hung@example.com", "password123", "Hung Nguyen");
        UserResponse response = new UserResponse(1L, "hung@example.com", "Hung Nguyen", LocalDateTime.now());

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("hung@example.com"))
                .andExpect(jsonPath("$.fullName").value("Hung Nguyen"));
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 400 Bad Request on blank email or short password")
    void register_InvalidPayload_Returns400() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("", "123", "");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 200 OK with JWT token when rate limit allows")
    void login_Success_Returns200() throws Exception {
        LoginRequest request = new LoginRequest("hung@example.com", "password123");
        AuthResponse response = new AuthResponse("mock.jwt.token", new UserResponse(1L, "hung@example.com", "Hung Nguyen", LocalDateTime.now()));

        when(loginLimiter.tryConsume(1)).thenReturn(true);
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 429 Too Many Requests when rate limit bucket is exhausted")
    void login_RateLimitExceeded_Returns429() throws Exception {
        LoginRequest request = new LoginRequest("hung@example.com", "password123");

        when(loginLimiter.tryConsume(1)).thenReturn(false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());
    }
}
