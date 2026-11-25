package com.medtracker.dto;

import com.medtracker.entity.Source;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineDTO {

    private Long id;

    @NotBlank(message = "Drug name is required")
    @Size(max = 200, message = "Drug name must not exceed 200 characters")
    private String drugName;

    @Size(max = 100, message = "Manufacturer must not exceed 100 characters")
    private String manufacturer;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @Size(max = 50, message = "Batch number must not exceed 50 characters")
    private String batchNumber;

    @Size(max = 100, message = "Barcode data must not exceed 100 characters")
    private String barcodeData;

    @NotNull(message = "Expiry date is required")
    @FutureOrPresent(message = "Expiry date must not be in the past")
    private LocalDate expiryDate;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    @Positive(message = "Price must be positive")
    @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 integer digits and 2 decimal places")
    private BigDecimal price;

    @Positive(message = "Low stock threshold must be positive")
    private Integer lowStockThreshold;

    @NotNull(message = "Source is required")
    private Source source;

    private Boolean isActive;
}
