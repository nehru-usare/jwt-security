package com.smart.jwtsecurity.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory rate limiter for /login.
 *
 * Limits requests per IP.
 * Production systems should replace this with Redis / Gateway-level limiting.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60_000; // 1 minute

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().equals("/login");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String ip = request.getRemoteAddr();
        long now = Instant.now().toEpochMilli();

        Attempt attempt = attempts.computeIfAbsent(ip, k -> new Attempt(now));

        if (now - attempt.startTime > WINDOW_MS) {
            attempt.reset(now);
        }

        attempt.count++;

        if (attempt.count > MAX_ATTEMPTS) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                {
                  "error": "TOO_MANY_REQUESTS",
                  "message": "Too many login attempts. Please try again later."
                }
            """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static class Attempt {
        long startTime;
        int count;

        Attempt(long startTime) {
            this.startTime = startTime;
            this.count = 0;
        }

        void reset(long newStart) {
            this.startTime = newStart;
            this.count = 0;
        }
    }
}
