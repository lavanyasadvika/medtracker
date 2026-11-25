package com.medtracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medtracker.dto.AdminSettingsRequest;
import com.medtracker.dto.AdminSettingsResponse;
import com.medtracker.dto.AdminUpdateUserRequest;
import com.medtracker.dto.AdminUserResponse;
import com.medtracker.dto.AuthResponse;
import com.medtracker.dto.RegisterRequest;
import com.medtracker.entity.Role;
import com.medtracker.exception.GlobalExceptionHandler;
import com.medtracker.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    private static final Long USER_ID = 5L;
    private static final String EMAIL = "demo.viewer@medtracker.local";
    private static final String NAME = "Demo Viewer";

    @Mock
    private AdminService adminService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminController(adminService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void should_list_users_when_admin_requests_users() throws Exception {
        when(adminService.listUsers()).thenReturn(List.of(buildUser(USER_ID, Boolean.TRUE)));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(USER_ID.intValue())))
                .andExpect(jsonPath("$[0].email", is(EMAIL)))
                .andExpect(jsonPath("$[0].role", is(Role.VIEWER.name())));
    }

    @Test
    void should_create_user_by_delegating_to_auth_service() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .token("jwt-token")
                .tokenType("Bearer")
                .expiresInMs(86400000L)
                .role(Role.PHARMACIST)
                .name("Demo Pharmacist")
                .email("demo.pharmacist@medtracker.local")
                .build();
        when(adminService.createUser(any(RegisterRequest.class))).thenReturn(response);

        RegisterRequest request = RegisterRequest.builder()
                .name("Demo Pharmacist")
                .email("demo.pharmacist@medtracker.local")
                .password("Pharmacist@123")
                .role(Role.PHARMACIST)
                .build();

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("demo.pharmacist@medtracker.local")))
                .andExpect(jsonPath("$.role", is(Role.PHARMACIST.name())));
    }

    @Test
    void should_update_user_name_and_role() throws Exception {
        AdminUserResponse response = buildUser(USER_ID, Boolean.TRUE);
        response.setName("Updated Viewer");
        response.setRole(Role.PHARMACIST);
        when(adminService.updateUser(any(Long.class), any(AdminUpdateUserRequest.class))).thenReturn(response);

        AdminUpdateUserRequest request = AdminUpdateUserRequest.builder()
                .name("Updated Viewer")
                .role(Role.PHARMACIST)
                .build();

        mockMvc.perform(put("/admin/users/{id}", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Viewer")))
                .andExpect(jsonPath("$.role", is(Role.PHARMACIST.name())));
    }

    @Test
    void should_set_is_active_false_when_deactivating_user() throws Exception {
        AdminUserResponse user = buildUser(USER_ID, Boolean.TRUE);
        doAnswer(invocation -> {
            user.setIsActive(Boolean.FALSE);
            return user;
        }).when(adminService).deactivateUser(USER_ID);

        mockMvc.perform(patch("/admin/users/{id}/deactivate", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive", is(false)));

        assertFalse(user.getIsActive());
        verify(adminService).deactivateUser(USER_ID);
    }

    @Test
    void should_return_settings() throws Exception {
        when(adminService.getSettings()).thenReturn(AdminSettingsResponse.builder()
                .expiryLeadDays(30)
                .defaultLowStockThreshold(10)
                .build());

        mockMvc.perform(get("/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiryLeadDays", is(30)))
                .andExpect(jsonPath("$.defaultLowStockThreshold", is(10)));
    }

    @Test
    void should_update_settings() throws Exception {
        when(adminService.updateSettings(any(AdminSettingsRequest.class))).thenReturn(AdminSettingsResponse.builder()
                .expiryLeadDays(45)
                .defaultLowStockThreshold(8)
                .build());

        AdminSettingsRequest request = AdminSettingsRequest.builder()
                .expiryLeadDays(45)
                .defaultLowStockThreshold(8)
                .build();

        mockMvc.perform(put("/admin/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiryLeadDays", is(45)))
                .andExpect(jsonPath("$.defaultLowStockThreshold", is(8)));
    }

    @Test
    void should_return_forbidden_when_non_admin_hits_admin_endpoint() throws Exception {
        doThrow(new AccessDeniedException("Access Denied"))
                .when(adminService).listUsers();

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_keep_admin_only_preauthorize_on_controller() {
        PreAuthorize preAuthorize = AdminController.class.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('ADMIN')", preAuthorize.value());
    }

    private AdminUserResponse buildUser(Long id, Boolean isActive) {
        return AdminUserResponse.builder()
                .id(id)
                .name(NAME)
                .email(EMAIL)
                .role(Role.VIEWER)
                .isActive(isActive)
                .build();
    }
}
