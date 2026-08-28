package com.taskflow.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-test-secret!!";

    private final JwtService jwtService =
            new JwtService(new JwtProperties(SECRET, 15, 30, "taskflow-api"));

    @Test
    void issuesATokenThatValidatesAndCarriesTheUserId() {
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateAccessToken(userId, "user@example.com");

        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    void rejectsATamperedToken() {
        String token = jwtService.generateAccessToken(UUID.randomUUID(), "user@example.com");
        String tampered = token.substring(0, token.length() - 3) + "abc";

        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    void rejectsATokenSignedWithADifferentKey() {
        JwtService other = new JwtService(new JwtProperties("another-secret-another-secret-xxxxxxxxxxxx", 15, 30, "x"));
        String foreignToken = other.generateAccessToken(UUID.randomUUID(), "user@example.com");

        assertThat(jwtService.isValid(foreignToken)).isFalse();
    }

    @Test
    void rejectsAnExpiredToken() {
        JwtService alreadyExpired = new JwtService(new JwtProperties(SECRET, -1, 30, "taskflow-api"));
        String token = alreadyExpired.generateAccessToken(UUID.randomUUID(), "user@example.com");

        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void refreshExpiryHonoursTheConfiguredWindow() {
        Instant expiry = jwtService.refreshTokenExpiry();

        assertThat(expiry).isCloseTo(Instant.now().plus(30, ChronoUnit.DAYS), within(5, ChronoUnit.SECONDS));
    }
}
