package com.medtracker.service;

import com.medtracker.config.AlertProperties;
import com.medtracker.dto.AdminSettingsRequest;
import com.medtracker.dto.AdminSettingsResponse;
import com.medtracker.dto.AdminUpdateUserRequest;
import com.medtracker.dto.AdminUserResponse;
import com.medtracker.dto.AuthResponse;
import com.medtracker.dto.RegisterRequest;
import com.medtracker.entity.User;
import com.medtracker.exception.ResourceNotFoundException;
import com.medtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final AlertProperties alertProperties;

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {
        return userRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(User::getId))
                .map(this::toAdminUserResponse)
                .toList();
    }

    @Transactional
    public AuthResponse createUser(RegisterRequest request) {
        return authService.register(request);
    }

    @Transactional
    public AdminUserResponse updateUser(Long id, AdminUpdateUserRequest request) {
        User user = findUser(id);
        user.setFullName(request.getName().trim());
        user.setRole(request.getRole());
        User savedUser = userRepository.save(user);
        log.info("Admin updated user id {} to role {}", savedUser.getId(), savedUser.getRole());
        return toAdminUserResponse(savedUser);
    }

    @Transactional
    public AdminUserResponse deactivateUser(Long id) {
        User user = findUser(id);
        user.setIsActive(Boolean.FALSE);
        User savedUser = userRepository.save(user);
        log.info("Admin deactivated user id {}", savedUser.getId());
        return toAdminUserResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AdminSettingsResponse getSettings() {
        return buildSettingsResponse();
    }

    @Transactional
    public AdminSettingsResponse updateSettings(AdminSettingsRequest request) {
        /*
         * Demo/v1 approach: settings are initially backed by application.yml through
         * AlertProperties (@ConfigurationProperties). PUT mutates that singleton bean at
         * runtime so reports and scheduled alerts immediately use the new values. The
         * values are intentionally in-memory only and reset on application restart; a
         * durable settings table can replace this without changing the controller contract.
         */
        alertProperties.setExpiryLeadDays(request.getExpiryLeadDays());
        alertProperties.setDefaultLowStockThreshold(request.getDefaultLowStockThreshold());
        log.info("Admin updated runtime settings: expiryLeadDays={}, defaultLowStockThreshold={}",
                alertProperties.getExpiryLeadDays(), alertProperties.getDefaultLowStockThreshold());
        return buildSettingsResponse();
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private AdminSettingsResponse buildSettingsResponse() {
        return AdminSettingsResponse.builder()
                .expiryLeadDays(alertProperties.getExpiryLeadDays())
                .defaultLowStockThreshold(alertProperties.getDefaultLowStockThreshold())
                .build();
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .name(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
