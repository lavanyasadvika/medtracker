package com.medtracker.controller;

import com.medtracker.dto.MedicineDTO;
import com.medtracker.service.MedicineService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/medicines")
@RequiredArgsConstructor
@Validated
public class MedicineController {

    private final MedicineService medicineService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    public ResponseEntity<MedicineDTO> createMedicine(@Valid @RequestBody MedicineDTO medicineDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicineService.createMedicine(medicineDTO));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','VIEWER','USER')")
    public ResponseEntity<List<MedicineDTO>> listMedicines(
            @RequestParam(required = false) @Min(value = 0, message = "Expiring-in-days filter must be 0 or greater") Integer expiringInDays,
            @RequestParam(required = false) Boolean lowStock,
            @RequestParam(required = false, name = "q") String query) {
        return ResponseEntity.ok(medicineService.listMedicines(expiringInDays, lowStock, query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','VIEWER','USER')")
    public ResponseEntity<MedicineDTO> getMedicine(@PathVariable Long id) {
        return ResponseEntity.ok(medicineService.getMedicine(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    public ResponseEntity<MedicineDTO> updateMedicine(
            @PathVariable Long id,
            @Valid @RequestBody MedicineDTO medicineDTO) {
        return ResponseEntity.ok(medicineService.updateMedicine(id, medicineDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Void> deleteMedicine(@PathVariable Long id) {
        medicineService.softDeleteMedicine(id);
        return ResponseEntity.noContent().build();
    }
}
