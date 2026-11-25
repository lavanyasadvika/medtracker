package com.medtracker.service;

import com.medtracker.entity.Medicine;
import com.medtracker.entity.Role;
import com.medtracker.entity.Source;
import com.medtracker.entity.User;
import com.medtracker.repository.UserRepository;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private UserRepository userRepository;

    private EmailService emailService;
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(javaMailSender, userRepository);
        ReflectionTestUtils.setField(emailService, "fromAddress", "alerts@medtracker.local");
        ReflectionTestUtils.setField(emailService, "fromName", "MedTracker");
        mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    }

    @Test
    void should_send_alert_only_to_active_admin_and_pharmacist_recipients() throws Exception {
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(userRepository.findByIsActiveTrueAndRoleIn(anyCollection())).thenReturn(List.of(
                buildUser("admin@medtracker.local", Role.ADMIN),
                buildUser("pharmacist@medtracker.local", Role.PHARMACIST)
        ));

        emailService.sendLowStockAlert(List.of(buildMedicine()));

        ArgumentCaptor<Collection<Role>> rolesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(userRepository).findByIsActiveTrueAndRoleIn(rolesCaptor.capture());
        assertTrue(rolesCaptor.getValue().contains(Role.ADMIN));
        assertTrue(rolesCaptor.getValue().contains(Role.PHARMACIST));
        assertEquals(2, rolesCaptor.getValue().size());
        assertArrayEquals(
                new String[]{"admin@medtracker.local", "pharmacist@medtracker.local"},
                List.of(mimeMessage.getRecipients(Message.RecipientType.TO))
                        .stream()
                        .map(Object::toString)
                        .toArray(String[]::new)
        );
        verify(javaMailSender).send(mimeMessage);
    }

    private User buildUser(String email, Role role) {
        return User.builder()
                .id(1L)
                .email(email)
                .fullName("Alert User")
                .passwordHash("hash")
                .role(role)
                .isActive(Boolean.TRUE)
                .build();
    }

    private Medicine buildMedicine() {
        return Medicine.builder()
                .id(1L)
                .userId(1L)
                .drugName("Paracetamol")
                .manufacturer("MedLabs")
                .batchNumber("BATCH-001")
                .expiryDate(LocalDate.now().plusMonths(3))
                .quantity(4)
                .lowStockThreshold(5)
                .source(Source.MANUAL)
                .isActive(Boolean.TRUE)
                .createdBy(1L)
                .updatedBy(1L)
                .build();
    }
}
