package com.finguard.service;

import com.finguard.dto.*;
import com.finguard.exception.UnauthorizedException;
import com.finguard.model.User;
import com.finguard.repository.UserRepository;
import com.finguard.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * AuthService – handles user registration, login, and profile retrieval.
 *
 * Security guarantees:
 *   - Passwords are BCrypt-hashed (cost 12)
 *   - Passwords are NEVER stored or returned in plain text
 *   - JWT tokens are generated and validated via JwtService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    // ─── Registration ─────────────────────────────────────────

    public LoginResponse register(RegisterRequest request) {
        // Validate passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Check email uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                "Email address is already registered: " + request.getEmail());
        }

        User user = User.builder()
            .name(request.getName())
            .email(request.getEmail().toLowerCase().trim())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role(User.Role.USER)
            .build();

        User saved = userRepository.save(user);
        log.info("New user registered: userId={} email={}", saved.getId(), saved.getEmail());

        return buildLoginResponse(saved);
    }

    // ─── Login ────────────────────────────────────────────────

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail().toLowerCase().trim(),
                    request.getPassword()
                )
            );
        } catch (Exception ex) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
            .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        log.info("User logged in: userId={} email={}", user.getId(), user.getEmail());
        return buildLoginResponse(user);
    }

    // ─── Current user ─────────────────────────────────────────

    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    // ─── Helpers ──────────────────────────────────────────────

    private LoginResponse buildLoginResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        // Embed role as a JWT claim
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().name());
        extraClaims.put("userId", user.getId());

        String token = jwtService.generateToken(extraClaims, userDetails);

        return LoginResponse.of(
            token, user.getIdString(), user.getName(),
            user.getEmail(), user.getRole().name(),
            jwtService.getExpirationMs()
        );
    }
}
