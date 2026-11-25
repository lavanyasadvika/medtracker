package com.medtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportInventoryItemDTO {

    private String name;
    private String category;
    private Integer quantity;
    private LocalDate expiry;
    private String manufacturer;
}
