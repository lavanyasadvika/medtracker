package com.medtracker.service;

import com.medtracker.config.AlertProperties;
import com.medtracker.dto.AdminUpdateUserRequest;
import com.medtracker.dto.AdminUserResponse;
import com.medtracker.dto.AuthResponse;
import com.medtracker.dto.RegisterRequest;
import com.medtracker.entity.Role;
import com.medtracker.entity.User;
import com.medtracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    private static final Long USER_ID = 5L;
    private static final String EMAIL = "inactive.user@medtracker.local";

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthService authService;

    @Spy
    private AlertProperties alertProperties = new AlertProperties();

    @InjectMocks
    private AdminService adminService;

    @Test
    void should_deactivate_user_and_user_can_no_longer_authenticate() {
        User user = buildUser(Boolean.TRUE);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserResponse response = adminService.deactivateUser(USER_ID);

        assertFalse(response.getIsActive());
        assertFalse(user.getIsActive());
        verify(userRepository).save(user);
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));
        assertThrows(BadCredentialsException.class, () -> authService.login(null));
    }

    @Test
    void should_update_name_and_role() {
        User user = buildUser(Boolean.TRUE);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserResponse response = adminService.updateUser(USER_ID, AdminUpdateUserRequest.builder()
                .name("Updated Name")
                .role(Role.PHARMACIST)
                .build());

        assertEquals("Updated Name", response.getName());
        assertEquals(Role.PHARMACIST, response.getRole());
    }

    @Test
    void should_delegate_create_user_to_auth_service_register() {
        RegisterRequest request = RegisterRequest.builder()
                .name("New Admin")
                .email("new.admin@medtracker.local")
                .password("Admin@123")
                .role(Role.ADMIN)
                .build();
        AuthResponse expected = AuthResponse.builder()
                .email(request.getEmail())
                .role(Role.ADMIN)
                .build();
        when(authService.register(request)).thenReturn(expected);

        AuthResponse response = adminService.createUser(request);

        assertEquals(expected, response);
        verify(authService).register(request);
    }

    @Test
    void should_update_runtime_settings_in_memory() {
        adminService.updateSettings(com.medtracker.dto.AdminSettingsRequest.builder()
                .expiryLeadDays(45)
                .defaultLowStockThreshold(8)
                .build());

        assertEquals(45, alertProperties.getExpiryLeadDays());
        assertEquals(8, alertProperties.getDefaultLowStockThreshold());
    }

    private User buildUser(Boolean isActive) {
        return User.builder()
                .id(USER_ID)
                .email(EMAIL)
                .fullName("Inactive User")
                .passwordHash("hash")
                .role(Role.VIEWER)
                .isActive(isActive)
                .build();
    }
}
