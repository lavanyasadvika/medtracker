package com.medtracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medtracker.dto.MedicineDTO;
import com.medtracker.entity.Source;
import com.medtracker.exception.GlobalExceptionHandler;
import com.medtracker.service.MedicineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MedicineControllerTest {

    private static final Long MEDICINE_ID = 1L;
    private static final String PHARMACIST_ROLE = "ROLE_PHARMACIST";
    private static final String VIEWER_ROLE = "ROLE_VIEWER";

    @Mock
    private MedicineService medicineService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MedicineController(medicineService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_create_medicine_when_user_is_pharmacist() throws Exception {
        MedicineDTO request = buildMedicine(null, Boolean.TRUE);
        MedicineDTO response = buildMedicine(MEDICINE_ID, Boolean.TRUE);
        when(medicineService.createMedicine(any(MedicineDTO.class))).thenReturn(response);
        authenticateAs(PHARMACIST_ROLE);

        mockMvc.perform(post("/medicines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(MEDICINE_ID.intValue())))
                .andExpect(jsonPath("$.drugName", is("Paracetamol")))
                .andExpect(jsonPath("$.source", is(Source.MANUAL.name())));
    }

    @Test
    void should_return_forbidden_when_viewer_creates_medicine() throws Exception {
        doThrow(new AccessDeniedException("Access Denied"))
                .when(medicineService).createMedicine(any(MedicineDTO.class));
        authenticateAs(VIEWER_ROLE);

        mockMvc.perform(post("/medicines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildMedicine(null, Boolean.TRUE))))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_return_forbidden_when_pharmacist_deletes_medicine() throws Exception {
        doThrow(new AccessDeniedException("Access Denied"))
                .when(medicineService).softDeleteMedicine(MEDICINE_ID);
        authenticateAs(PHARMACIST_ROLE);

        mockMvc.perform(delete("/medicines/{id}", MEDICINE_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_soft_delete_medicine_by_setting_is_active_false() throws Exception {
        MedicineDTO medicine = buildMedicine(MEDICINE_ID, Boolean.TRUE);
        doAnswer(invocation -> {
            medicine.setIsActive(Boolean.FALSE);
            return null;
        }).when(medicineService).softDeleteMedicine(MEDICINE_ID);

        mockMvc.perform(delete("/medicines/{id}", MEDICINE_ID))
                .andExpect(status().isNoContent());

        assertFalse(medicine.getIsActive());
        verify(medicineService).softDeleteMedicine(MEDICINE_ID);
    }

    @Test
    void should_exclude_inactive_medicines_when_listing() throws Exception {
        MedicineDTO activeMedicine = buildMedicine(MEDICINE_ID, Boolean.TRUE);
        MedicineDTO inactiveMedicine = buildMedicine(2L, Boolean.FALSE);
        List<MedicineDTO> repositoryLikeData = List.of(activeMedicine, inactiveMedicine);
        List<MedicineDTO> activeOnly = repositoryLikeData.stream()
                .filter(MedicineDTO::getIsActive)
                .toList();
        when(medicineService.listMedicines(null, null, null)).thenReturn(activeOnly);

        mockMvc.perform(get("/medicines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(MEDICINE_ID.intValue())))
                .andExpect(jsonPath("$[0].isActive", is(true)));

        assertEquals(1, activeOnly.size());
        assertTrue(activeOnly.stream().noneMatch(medicine -> inactiveMedicine.getId().equals(medicine.getId())));
    }

    @Test
    void should_keep_required_preauthorize_role_matrix_on_controller_methods() throws Exception {
        assertEquals(
                "hasAnyRole('ADMIN','PHARMACIST')",
                MedicineController.class.getMethod("createMedicine", MedicineDTO.class)
                        .getAnnotation(PreAuthorize.class).value()
        );
        assertEquals(
                "hasAnyRole('ADMIN')",
                MedicineController.class.getMethod("deleteMedicine", Long.class)
                        .getAnnotation(PreAuthorize.class).value()
        );
    }

    private void authenticateAs(String role) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "user@medtracker.local",
                null,
                List.of(new SimpleGrantedAuthority(role))
        );
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private MedicineDTO buildMedicine(Long id, Boolean isActive) {
        return MedicineDTO.builder()
                .id(id)
                .drugName("Paracetamol")
                .manufacturer("MedLabs")
                .category("Analgesic")
                .batchNumber("BATCH-001")
                .barcodeData("8901234567890")
                .expiryDate(LocalDate.now().plusMonths(6))
                .quantity(25)
                .price(new BigDecimal("12.50"))
                .lowStockThreshold(5)
                .source(Source.MANUAL)
                .isActive(isActive)
                .build();
    }
}
