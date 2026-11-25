package com.medtracker.controller;

import com.medtracker.dto.DispenseRequest;
import com.medtracker.dto.MedicineDTO;
import com.medtracker.dto.RestockRequest;
import com.medtracker.service.MedicineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/medicines")
@RequiredArgsConstructor
public class StockController {

    private final MedicineService medicineService;

    @PostMapping("/{id}/dispense")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','VIEWER')")
    public ResponseEntity<MedicineDTO> dispenseMedicine(
            @PathVariable Long id,
            @Valid @RequestBody DispenseRequest dispenseRequest) {
        return ResponseEntity.ok(medicineService.dispenseMedicine(id, dispenseRequest.getQuantity()));
    }

    @PutMapping("/{id}/restock")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    public ResponseEntity<MedicineDTO> restockMedicine(
            @PathVariable Long id,
            @Valid @RequestBody RestockRequest restockRequest) {
        return ResponseEntity.ok(medicineService.restockMedicine(id, restockRequest.getQuantity()));
    }
}
