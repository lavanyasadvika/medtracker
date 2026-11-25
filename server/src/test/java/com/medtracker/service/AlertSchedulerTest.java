package com.medtracker.service;

import com.medtracker.config.AlertProperties;
import com.medtracker.entity.Medicine;
import com.medtracker.entity.Source;
import com.medtracker.repository.MedicineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertSchedulerTest {

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private EmailService emailService;

    private AlertProperties alertProperties;
    private AlertScheduler alertScheduler;

    @BeforeEach
    void setUp() {
        alertProperties = new AlertProperties();
        alertProperties.setExpiryLeadDays(30);
        alertProperties.setDefaultLowStockThreshold(10);
        alertScheduler = new AlertScheduler(medicineRepository, emailService, alertProperties);
    }

    @Test
    void should_call_email_service_when_alert_lists_are_non_empty() {
        Medicine expiringSoon = buildMedicine(1L, "Expiring Medicine", LocalDate.now().plusDays(10), 20, 5);
        Medicine expired = buildMedicine(2L, "Expired Medicine", LocalDate.now().minusDays(1), 20, 5);
        Medicine lowStock = buildMedicine(3L, "Low Stock Medicine", LocalDate.now().plusMonths(3), 4, 5);

        when(medicineRepository.findExpiringSoon(any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of(expiringSoon));
        when(medicineRepository.findExpired(any(LocalDate.class))).thenReturn(List.of(expired));
        when(medicineRepository.findLowStock(eq(10))).thenReturn(List.of(lowStock));

        alertScheduler.runScheduledAlerts();

        verify(emailService).sendExpiryAlert(List.of(expiringSoon), List.of(expired));
        verify(emailService).sendLowStockAlert(List.of(lowStock));
    }

    @Test
    void should_skip_email_service_when_alert_lists_are_empty() {
        when(medicineRepository.findExpiringSoon(any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());
        when(medicineRepository.findExpired(any(LocalDate.class))).thenReturn(List.of());
        when(medicineRepository.findLowStock(eq(10))).thenReturn(List.of());

        alertScheduler.runScheduledAlerts();

        verify(emailService, never()).sendExpiryAlert(any(), any());
        verify(emailService, never()).sendLowStockAlert(any());
    }

    private Medicine buildMedicine(Long id, String drugName, LocalDate expiryDate, Integer quantity, Integer threshold) {
        return Medicine.builder()
                .id(id)
                .userId(1L)
                .drugName(drugName)
                .manufacturer("MedLabs")
                .batchNumber("BATCH-" + id)
                .expiryDate(expiryDate)
                .quantity(quantity)
                .lowStockThreshold(threshold)
                .source(Source.MANUAL)
                .isActive(Boolean.TRUE)
                .createdBy(1L)
                .updatedBy(1L)
                .build();
    }
}
