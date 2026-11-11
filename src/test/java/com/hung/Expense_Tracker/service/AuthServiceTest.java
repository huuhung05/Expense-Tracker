package com.hung.Expense_Tracker.service;

import com.hung.Expense_Tracker.dto.AuthResponse;
import com.hung.Expense_Tracker.dto.LoginRequest;
import com.hung.Expense_Tracker.dto.RegisterRequest;
import com.hung.Expense_Tracker.dto.UserResponse;
import com.hung.Expense_Tracker.entity.User;
import com.hung.Expense_Tracker.exception.EmailAlreadyExistsException;
import com.hung.Expense_Tracker.repository.UserRepository;
import com.hung.Expense_Tracker.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Should successfully register a new user with hashed password")
    void register_Success() {
        RegisterRequest request = new RegisterRequest("hung@example.com", "password123", "Hung Nguyen");

        when(userRepository.existsByEmail("hung@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return User.builder()
                    .id(1L)
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .password(user.getPassword())
                    .createdAt(LocalDateTime.now())
                    .build();
        });

        UserResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("hung@example.com");
        assertThat(response.fullName()).isEqualTo("Hung Nguyen");

        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when registering with duplicate email")
    void register_DuplicateEmail_ThrowsException() {
        RegisterRequest request = new RegisterRequest("hung@example.com", "password123", "Hung Nguyen");

        when(userRepository.existsByEmail("hung@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("Email is already registered");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should authenticate and return JWT token on valid credentials")
    void login_Success() {
        LoginRequest request = new LoginRequest("hung@example.com", "password123");
        User user = User.builder()
                .id(1L)
                .email("hung@example.com")
                .fullName("Hung Nguyen")
                .password("hashed_password")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("hung@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("hung@example.com")).thenReturn("mocked.jwt.token");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("mocked.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo("hung@example.com");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when password does not match")
    void login_BadCredentials_ThrowsException() {
        LoginRequest request = new LoginRequest("hung@example.com", "wrongpass");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateToken(anyString());
    }
}
