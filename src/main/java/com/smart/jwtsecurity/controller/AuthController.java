package com.smart.jwtsecurity.controller;

import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.smart.jwtsecurity.dto.LoginApiResponse;
import com.smart.jwtsecurity.dto.LoginRequest;
import com.smart.jwtsecurity.util.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Authentication Controller.
 *
 * Handles JSON-based login and JWT issuance.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public LoginApiResponse login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpRequest
    ) {

        try {
            /*
             * 1️⃣ Delegate authentication to Spring Security
             */
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    request.getUsernameOrEmail(),
                                    request.getPassword()
                            )
                    );

            /*
             * 2️⃣ Authentication successful → extract principal
             */
            UserDetails user =
                    (UserDetails) authentication.getPrincipal();

            /*
             * 3️⃣ Generate JWT
             */
            String token = jwtUtil.generateToken(user);

            /*
             * 4️⃣ Build structured response
             */
            return LoginApiResponse.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .expiresInSeconds(jwtUtil.getExpirationSeconds())
                    .username(user.getUsername())
                    .roles(
                            user.getAuthorities()
                                    .stream()
                                    .map(a -> a.getAuthority())
                                    .collect(Collectors.toSet())
                    )
                    .build();

        } catch (BadCredentialsException ex) {

            log.warn(
                "LOGIN FAILED: invalid credentials | user={} | ip={}",
                request.getUsernameOrEmail(),
                httpRequest.getRemoteAddr()
            );
            throw ex;

        } catch (AuthenticationException ex) {

            log.warn(
                "LOGIN FAILED: authentication error | user={} | ip={} | reason={}",
                request.getUsernameOrEmail(),
                httpRequest.getRemoteAddr(),
                ex.getClass().getSimpleName()
            );
            throw ex;
        }
    }
}
