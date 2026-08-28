package com.taskflow.api.service;

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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration, login and refresh-token handling.
 *
 * <p>Access tokens are short-lived JWTs. Refresh tokens are opaque random strings: only their
 * SHA-256 hash is stored, so a database leak doesn't hand out usable tokens. Every refresh
 * rotates the token, and presenting an already-used one revokes the whole family as a
 * theft-detection measure.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with that email already exists");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().strip())
                .enabled(true)
                .build();
        userRepository.save(user);

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository
                .findByEmailIgnoreCase(normalizeEmail(request.email()))
                .filter(User::isEnabled)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(sha256Hex(request.refreshToken()))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (!stored.isValid()) {
            // A revoked or expired token showing up again is a strong signal of replay or
            // theft: drop every outstanding token for the user and force a fresh login.
            refreshTokenRepository.revokeAllForUser(stored.getUser());
            throw new BadCredentialsException("Refresh token is no longer valid");
        }

        stored.setRevoked(true);
        return issueTokens(stored.getUser());
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository
                .findByTokenHash(sha256Hex(request.refreshToken()))
                .ifPresent(token -> token.setRevoked(true));
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String rawRefreshToken = randomRefreshToken();

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(sha256Hex(rawRefreshToken))
                .expiresAt(jwtService.refreshTokenExpiry())
                .revoked(false)
                .build());

        return AuthResponse.bearer(accessToken, rawRefreshToken, jwtProperties.accessTokenTtlMinutes() * 60);
    }

    private String randomRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required but unavailable in this JVM", e);
        }
    }
}
