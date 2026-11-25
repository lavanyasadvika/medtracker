package com.medtracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medtracker.dto.AuthResponse;
import com.medtracker.dto.LoginRequest;
import com.medtracker.dto.RegisterRequest;
import com.medtracker.entity.Role;
import com.medtracker.exception.GlobalExceptionHandler;
import com.medtracker.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String TOKEN = "jwt-token";
    private static final String EMAIL = "admin@medtracker.local";
    private static final String NAME = "System Administrator";
    private static final String PASSWORD = "Admin@123";

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void should_return_token_when_login_successful() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .token(TOKEN)
                .tokenType("Bearer")
                .expiresInMs(86400000L)
                .role(Role.ADMIN)
                .name(NAME)
                .email(EMAIL)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        LoginRequest request = LoginRequest.builder()
                .email(EMAIL)
                .password(PASSWORD)
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is(TOKEN)))
                .andExpect(jsonPath("$.role", is(Role.ADMIN.name())))
                .andExpect(jsonPath("$.name", is(NAME)))
                .andExpect(jsonPath("$.email", is(EMAIL)));
    }

    @Test
    void should_return_unauthorized_when_login_password_is_bad() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest request = LoginRequest.builder()
                .email(EMAIL)
                .password("WrongPassword@123")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(HttpStatus.UNAUTHORIZED.value())))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    void should_return_conflict_when_register_email_already_exists() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "A user with this email already exists"));

        RegisterRequest request = RegisterRequest.builder()
                .name(NAME)
                .email(EMAIL)
                .password(PASSWORD)
                .role(Role.ADMIN)
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(HttpStatus.CONFLICT.value())))
                .andExpect(jsonPath("$.message", is("A user with this email already exists")));
    }
}
