package com.medtracker.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSettingsRequest {

    @NotNull(message = "Expiry lead days is required")
    @Min(value = 0, message = "Expiry lead days must be zero or greater")
    @Max(value = 3650, message = "Expiry lead days must not exceed 3650")
    private Integer expiryLeadDays;

    @NotNull(message = "Default low-stock threshold is required")
    @Min(value = 1, message = "Default low-stock threshold must be greater than zero")
    private Integer defaultLowStockThreshold;
}
