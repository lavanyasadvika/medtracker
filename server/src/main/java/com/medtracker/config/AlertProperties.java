package com.medtracker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.alerts")
public class AlertProperties {

    private String cron = "0 0 8 * * *";
    private Integer expiryLeadDays = 30;
    private Integer defaultLowStockThreshold = 10;
}
