package com.taskflow.api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "taskflow.jwt")
public record JwtProperties(String secret, long accessTokenTtlMinutes, long refreshTokenTtlDays, String issuer) {
}
