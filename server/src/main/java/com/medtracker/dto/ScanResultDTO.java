package com.medtracker.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.medtracker.entity.Source;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScanResultDTO {

    @Size(max = 200, message = "Drug name must not exceed 200 characters")
    private String drugName;

    @FutureOrPresent(message = "Expiry date must not be in the past")
    private LocalDate expiryDate;

    private Source source;

    @Size(max = 100, message = "Manufacturer must not exceed 100 characters")
    private String manufacturer;

    @Size(max = 100, message = "Barcode must not exceed 100 characters")
    private String barcode;

    private Boolean manualEntryRequired;

    @Size(max = 500, message = "Message must not exceed 500 characters")
    private String message;
}
