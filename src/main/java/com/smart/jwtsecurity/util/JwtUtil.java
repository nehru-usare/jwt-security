package com.smart.jwtsecurity.util;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.smart.jwtsecurity.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * JWT utility responsible for:
 * - Token generation
 * - Token validation
 * - Claim extraction
 *
 * Stateless, thread-safe, and production hardened.
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties properties;

    private Key signingKey;

    /**
     * Initialize signing key once at startup.
     * Prevents runtime key regeneration.
     */
    @PostConstruct
    void init() {
        this.signingKey = Keys.hmacShaKeyFor(
                properties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Generates a signed JWT access token.
     */
    public String generateToken(UserDetails user) {

        List<String> roles = user.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuer(properties.getIssuer())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + properties.getExpirationMs()))
                .claim("roles", roles)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validates token signature and standard claims.
     * Throws JwtException on ANY validation failure.
     */
    public Claims validateToken(String token) {

        return Jwts.parserBuilder()
                .requireIssuer(properties.getIssuer())
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public long getExpirationSeconds() {
        return properties.getExpirationMs() / 1000;
    }

}
