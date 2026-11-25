package com.medtracker.service;

import com.medtracker.dto.MedicineDTO;
import com.medtracker.entity.Medicine;
import com.medtracker.exception.ResourceNotFoundException;
import com.medtracker.repository.MedicineRepository;
import com.medtracker.security.CurrentUserProvider;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicineService {

    private static final String MEDICINE_RESOURCE = "Medicine";

    private final MedicineRepository medicineRepository;
    private final CurrentUserProvider currentUserProvider;

    @Transactional
    public MedicineDTO createMedicine(MedicineDTO medicineDTO) {
        validateExpiryDateIsNotPast(medicineDTO.getExpiryDate());
        Long currentUserId = currentUserProvider.getCurrentUserId();

        Medicine medicine = toEntity(medicineDTO);
        medicine.setId(null);
        medicine.setUserId(currentUserId);
        medicine.setIsActive(Boolean.TRUE);
        medicine.setCreatedBy(currentUserId);
        medicine.setUpdatedBy(currentUserId);

        Medicine savedMedicine = medicineRepository.save(medicine);
        log.info("Created medicine with id {} by user {}", savedMedicine.getId(), currentUserId);
        return toDto(savedMedicine);
    }

    @Transactional(readOnly = true)
    public List<MedicineDTO> listMedicines(Integer expiringInDays, Boolean lowStock, String query) {
        return medicineRepository.findAll(
                        buildActiveMedicineSpecification(expiringInDays, Boolean.TRUE.equals(lowStock), normalizeQuery(query)),
                        Sort.by(Sort.Direction.ASC, "drugName"))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public MedicineDTO getMedicine(Long id) {
        return medicineRepository.findByIdAndIsActiveTrue(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(MEDICINE_RESOURCE, "id", id));
    }

    @Transactional
    public MedicineDTO updateMedicine(Long id, MedicineDTO medicineDTO) {
        Medicine medicine = medicineRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(MEDICINE_RESOURCE, "id", id));

        updateEntity(medicine, medicineDTO);
        medicine.setUpdatedBy(currentUserProvider.getCurrentUserId());

        Medicine savedMedicine = medicineRepository.save(medicine);
        log.info("Updated medicine with id {}", savedMedicine.getId());
        return toDto(savedMedicine);
    }

    @Transactional
    public void softDeleteMedicine(Long id) {
        Medicine medicine = medicineRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(MEDICINE_RESOURCE, "id", id));

        medicine.setIsActive(Boolean.FALSE);
        medicine.setUpdatedBy(currentUserProvider.getCurrentUserId());
        medicineRepository.save(medicine);
        log.info("Soft-deleted medicine with id {}", id);
    }

    @Transactional
    public MedicineDTO dispenseMedicine(Long id, Integer quantity) {
        Medicine medicine = medicineRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(MEDICINE_RESOURCE, "id", id));

        if (quantity > medicine.getQuantity()) {
            throw new com.medtracker.exception.InsufficientStockException(
                    medicine.getDrugName(),
                    quantity,
                    medicine.getQuantity());
        }

        medicine.setQuantity(medicine.getQuantity() - quantity);
        medicine.setUpdatedBy(currentUserProvider.getCurrentUserId());

        Medicine savedMedicine = medicineRepository.save(medicine);
        log.info("Dispensed {} units from medicine id {}", quantity, id);
        return toDto(savedMedicine);
    }

    @Transactional
    public MedicineDTO restockMedicine(Long id, Integer quantity) {
        Medicine medicine = medicineRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(MEDICINE_RESOURCE, "id", id));

        medicine.setQuantity(medicine.getQuantity() + quantity);
        medicine.setUpdatedBy(currentUserProvider.getCurrentUserId());

        Medicine savedMedicine = medicineRepository.save(medicine);
        log.info("Restocked {} units into medicine id {}", quantity, id);
        return toDto(savedMedicine);
    }

    private void validateExpiryDateIsNotPast(LocalDate expiryDate) {
        if (expiryDate != null && expiryDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Expiry date must not be in the past");
        }
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return query.trim();
    }

    private Specification<Medicine> buildActiveMedicineSpecification(
            Integer expiringInDays,
            boolean lowStock,
            String query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isTrue(root.get("isActive")));

            if (expiringInDays != null) {
                LocalDate today = LocalDate.now();
                predicates.add(criteriaBuilder.between(root.get("expiryDate"), today, today.plusDays(expiringInDays)));
            }

            if (lowStock) {
                predicates.add(criteriaBuilder.isNotNull(root.get("lowStockThreshold")));
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("quantity"), root.get("lowStockThreshold")));
            }

            if (query != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("drugName")),
                        "%" + query.toLowerCase(Locale.ROOT) + "%"));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Medicine toEntity(MedicineDTO medicineDTO) {
        return Medicine.builder()
                .drugName(medicineDTO.getDrugName())
                .manufacturer(medicineDTO.getManufacturer())
                .category(medicineDTO.getCategory())
                .batchNumber(medicineDTO.getBatchNumber())
                .barcodeData(medicineDTO.getBarcodeData())
                .expiryDate(medicineDTO.getExpiryDate())
                .quantity(medicineDTO.getQuantity())
                .price(medicineDTO.getPrice())
                .lowStockThreshold(medicineDTO.getLowStockThreshold())
                .source(medicineDTO.getSource())
                .isActive(Boolean.TRUE)
                .build();
    }

    private void updateEntity(Medicine medicine, MedicineDTO medicineDTO) {
        medicine.setDrugName(medicineDTO.getDrugName());
        medicine.setManufacturer(medicineDTO.getManufacturer());
        medicine.setCategory(medicineDTO.getCategory());
        medicine.setBatchNumber(medicineDTO.getBatchNumber());
        medicine.setBarcodeData(medicineDTO.getBarcodeData());
        medicine.setExpiryDate(medicineDTO.getExpiryDate());
        medicine.setQuantity(medicineDTO.getQuantity());
        medicine.setPrice(medicineDTO.getPrice());
        medicine.setLowStockThreshold(medicineDTO.getLowStockThreshold());
        medicine.setSource(medicineDTO.getSource());
    }

    private MedicineDTO toDto(Medicine medicine) {
        return MedicineDTO.builder()
                .id(medicine.getId())
                .drugName(medicine.getDrugName())
                .manufacturer(medicine.getManufacturer())
                .category(medicine.getCategory())
                .batchNumber(medicine.getBatchNumber())
                .barcodeData(medicine.getBarcodeData())
                .expiryDate(medicine.getExpiryDate())
                .quantity(medicine.getQuantity())
                .price(medicine.getPrice())
                .lowStockThreshold(medicine.getLowStockThreshold())
                .source(medicine.getSource())
                .isActive(medicine.getIsActive())
                .build();
    }
}
