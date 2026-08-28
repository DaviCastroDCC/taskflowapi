package com.taskflow.api.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Caps login and registration attempts per client IP to blunt credential-stuffing and
 * account-enumeration attempts. State is in-memory, which is fine for a single instance;
 * a multi-node deployment would back this with Redis instead.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> PROTECTED_PATHS = Set.of("/api/v1/auth/login", "/api/v1/auth/register");

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {

        if (!PROTECTED_PATHS.contains(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        Bucket bucket = buckets.computeIfAbsent(clientIp(request), ip -> newBucket());
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"detail\":\"Too many attempts. Try again in a minute.\"}");
        }
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(1)).build())
                .build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
