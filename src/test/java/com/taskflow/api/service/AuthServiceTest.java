package com.taskflow.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.taskflow.api.domain.RefreshToken;
import com.taskflow.api.domain.User;
import com.taskflow.api.dto.request.LoginRequest;
import com.taskflow.api.dto.request.RefreshRequest;
import com.taskflow.api.dto.request.RegisterRequest;
import com.taskflow.api.dto.response.AuthResponse;
import com.taskflow.api.exception.ConflictException;
import com.taskflow.api.repository.RefreshTokenRepository;
import com.taskflow.api.repository.UserRepository;
import com.taskflow.api.security.JwtProperties;
import com.taskflow.api.security.JwtService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private final JwtService jwtService =
            new JwtService(new JwtProperties("test-secret-test-secret-test-secret-xxxx", 15, 30, "taskflow-api"));
    private final JwtProperties jwtProperties =
            new JwtProperties("test-secret-test-secret-test-secret-xxxx", 15, 30, "taskflow-api");

    private AuthService authService() {
        return new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtService, jwtProperties);
    }

    @Test
    void registerRejectsAnAlreadyUsedEmail() {
        when(userRepository.existsByEmailIgnoreCase("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService().register(new RegisterRequest("Dup@Example.com", "password123", "Dup")))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerStoresOnlyTheHashOfTheRefreshTokenAndReturnsABearerPair() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
            return user;
        });
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService().register(new RegisterRequest("new@example.com", "password123", "New User"));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.expiresInSeconds()).isEqualTo(15 * 60);

        ArgumentCaptor<RefreshToken> storedToken = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(storedToken.capture());
        assertThat(storedToken.getValue().getTokenHash())
                .hasSize(64)
                .isNotEqualTo(response.refreshToken());
        assertThat(storedToken.getValue().isRevoked()).isFalse();
        assertThat(storedToken.getValue().getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void loginRejectsAWrongPassword() {
        User user = enabledUser();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService().login(new LoginRequest("user@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginRejectsADisabledAccount() {
        User user = enabledUser();
        user.setEnabled(false);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService().login(new LoginRequest("user@example.com", "whatever")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void presentingAnAlreadyRevokedRefreshTokenWipesTheWholeFamily() {
        User user = enabledUser();
        RefreshToken revoked = RefreshToken.builder()
                .user(user)
                .tokenHash("does-not-matter")
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(true)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> authService().refresh(new RefreshRequest("stolen-token")))
                .isInstanceOf(BadCredentialsException.class);
        verify(refreshTokenRepository).revokeAllForUser(user);
    }

    @Test
    void refreshRotatesTheStoredToken() {
        User user = enabledUser();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        RefreshToken active = RefreshToken.builder()
                .user(user)
                .tokenHash("current-hash")
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(active));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService().refresh(new RefreshRequest("current-token"));

        assertThat(active.isRevoked()).isTrue();
        assertThat(response.refreshToken()).isNotBlank();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    private User enabledUser() {
        User user = User.builder()
                .email("user@example.com")
                .passwordHash("stored-hash")
                .fullName("User")
                .enabled(true)
                .build();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }
}
