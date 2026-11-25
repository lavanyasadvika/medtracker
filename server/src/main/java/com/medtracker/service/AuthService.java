package com.medtracker.service;

import com.medtracker.dto.AuthResponse;
import com.medtracker.dto.LoginRequest;
import com.medtracker.dto.RegisterRequest;
import com.medtracker.entity.Role;
import com.medtracker.entity.User;
import com.medtracker.repository.UserRepository;
import com.medtracker.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${app.jwt.expiration-ms:86400000}")
    private Long jwtExpirationMs;

    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this email already exists");
        }

        Role requestedRole = request.getRole() == null ? Role.USER : request.getRole();
        User user = User.builder()
                .fullName(request.getName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(requestedRole)
                .isActive(Boolean.TRUE)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered user with email {} and role {}", savedUser.getEmail(), savedUser.getRole());
        return buildAuthResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
        );

        User user = findActiveUserByEmail(normalizedEmail);
        return buildAuthResponse(user);
    }

    public AuthResponse getCurrentUserProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        User user = findActiveUserByEmail(authentication.getName());
        return buildProfileResponse(user);
    }

    private User findActiveUserByEmail(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User account is inactive");
        }

        return user;
    }

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .token(jwtUtil.generateToken(user.getEmail(), user.getRole()))
                .tokenType(TOKEN_TYPE)
                .expiresInMs(jwtExpirationMs)
                .role(user.getRole())
                .name(user.getFullName())
                .email(user.getEmail())
                .build();
    }

    private AuthResponse buildProfileResponse(User user) {
        return AuthResponse.builder()
                .role(user.getRole())
                .name(user.getFullName())
                .email(user.getEmail())
                .build();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
