package com.medtracker.controller;

import com.medtracker.dto.ReportExpiryDTO;
import com.medtracker.dto.ReportExpiryItemDTO;
import com.medtracker.dto.ReportInventoryItemDTO;
import com.medtracker.exception.GlobalExceptionHandler;
import com.medtracker.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ReportController(reportService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void should_return_inventory_report() throws Exception {
        when(reportService.getInventoryReport()).thenReturn(List.of(buildInventoryItem("Paracetamol")));

        mockMvc.perform(get("/reports/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Paracetamol")))
                .andExpect(jsonPath("$[0].category", is("Analgesic")))
                .andExpect(jsonPath("$[0].quantity", is(5)))
                .andExpect(jsonPath("$[0].manufacturer", is("MedLabs")));
    }

    @Test
    void should_return_expiry_report_buckets() throws Exception {
        when(reportService.getExpiryReport()).thenReturn(ReportExpiryDTO.builder()
                .expired(List.of(buildExpiryItem("Expired Med", "red")))
                .expiring(List.of(buildExpiryItem("Expiring Med", "amber")))
                .safe(List.of(buildExpiryItem("Safe Med", "green")))
                .build());

        mockMvc.perform(get("/reports/expiry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expired[0].status", is("red")))
                .andExpect(jsonPath("$.expiring[0].status", is("amber")))
                .andExpect(jsonPath("$.safe[0].status", is("green")));
    }

    @Test
    void should_return_low_stock_report() throws Exception {
        when(reportService.getLowStockReport()).thenReturn(List.of(buildInventoryItem("Low Stock Med")));

        mockMvc.perform(get("/reports/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Low Stock Med")));
    }

    @Test
    void should_export_csv_with_content_disposition() throws Exception {
        when(reportService.exportReport("inventory", "csv"))
                .thenReturn(new ReportService.ReportExport(
                        "medtracker-inventory-report.csv",
                        "text/csv",
                        "Name\nParacetamol\n".getBytes()));

        mockMvc.perform(get("/reports/inventory/export").param("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("medtracker-inventory-report.csv")));
    }

    @Test
    void should_export_pdf_with_content_disposition() throws Exception {
        when(reportService.exportReport("low-stock", "pdf"))
                .thenReturn(new ReportService.ReportExport(
                        "medtracker-low-stock-report.pdf",
                        MediaType.APPLICATION_PDF_VALUE,
                        new byte[]{37, 80, 68, 70}));

        mockMvc.perform(get("/reports/low-stock/export").param("format", "pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("medtracker-low-stock-report.pdf")));
    }

    @Test
    void should_return_forbidden_when_viewer_exports_report() throws Exception {
        doThrow(new AccessDeniedException("Access Denied"))
                .when(reportService).exportReport("inventory", "csv");

        mockMvc.perform(get("/reports/inventory/export").param("format", "csv"))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_keep_report_role_matrix_on_controller_methods() throws Exception {
        assertEquals(
                "hasAnyRole('ADMIN','PHARMACIST','VIEWER','USER')",
                ReportController.class.getMethod("getInventoryReport")
                        .getAnnotation(PreAuthorize.class).value()
        );
        assertEquals(
                "hasAnyRole('ADMIN','PHARMACIST')",
                ReportController.class.getMethod("exportReport", String.class, String.class)
                        .getAnnotation(PreAuthorize.class).value()
        );
    }

    private ReportInventoryItemDTO buildInventoryItem(String name) {
        return ReportInventoryItemDTO.builder()
                .name(name)
                .category("Analgesic")
                .quantity(5)
                .expiry(LocalDate.now().plusDays(10))
                .manufacturer("MedLabs")
                .build();
    }

    private ReportExpiryItemDTO buildExpiryItem(String name, String status) {
        return ReportExpiryItemDTO.builder()
                .name(name)
                .category("Analgesic")
                .quantity(5)
                .expiry(LocalDate.now().plusDays(10))
                .manufacturer("MedLabs")
                .status(status)
                .build();
    }
}
