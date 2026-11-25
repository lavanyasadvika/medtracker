package com.medtracker.service;

import com.medtracker.config.AlertProperties;
import com.medtracker.entity.Medicine;
import com.medtracker.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertScheduler {

    private final MedicineRepository medicineRepository;
    private final EmailService emailService;
    private final AlertProperties alertProperties;

    @Scheduled(cron = "${app.alerts.cron:0 0 8 * * *}")
    @Transactional(readOnly = true)
    public void runScheduledAlerts() {
        LocalDate today = LocalDate.now();
        LocalDate expiryWindowEnd = today.plusDays(alertProperties.getExpiryLeadDays());

        List<Medicine> expiringSoon = medicineRepository.findExpiringSoon(today, expiryWindowEnd);
        List<Medicine> expired = medicineRepository.findExpired(today);
        List<Medicine> lowStock = medicineRepository.findLowStock(alertProperties.getDefaultLowStockThreshold());

        if (!expiringSoon.isEmpty() || !expired.isEmpty()) {
            emailService.sendExpiryAlert(expiringSoon, expired);
        }

        if (!lowStock.isEmpty()) {
            emailService.sendLowStockAlert(lowStock);
        }

        log.info(
                "Scheduled alerts completed: expiringSoon={}, expired={}, lowStock={}",
                expiringSoon.size(),
                expired.size(),
                lowStock.size());
    }
}
