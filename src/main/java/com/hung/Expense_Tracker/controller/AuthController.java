package com.hung.Expense_Tracker.controller;

import com.hung.Expense_Tracker.dto.AuthResponse;
import com.hung.Expense_Tracker.dto.LoginRequest;
import com.hung.Expense_Tracker.dto.RegisterRequest;
import com.hung.Expense_Tracker.dto.UserResponse;
import com.hung.Expense_Tracker.service.AuthService;
import io.github.bucket4j.Bucket;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Rate-limiter bean (5 requests/min) protecting the login endpoint from
     * brute-force attacks.
     * Injected by name via {@code @Qualifier} to avoid ambiguity if other Bucket
     * beans are added later.
     */
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final @Qualifier("loginLimiter") Bucket loginLimiter;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // Rate-limit: reject with 429 Too Many Requests if bucket is exhausted
        if (!loginLimiter.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        return ResponseEntity.ok(authService.login(request));
    }
}
