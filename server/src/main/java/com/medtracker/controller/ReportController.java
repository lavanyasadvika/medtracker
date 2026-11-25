package com.medtracker.controller;

import com.medtracker.dto.ReportExpiryDTO;
import com.medtracker.dto.ReportInventoryItemDTO;
import com.medtracker.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/inventory")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','VIEWER','USER')")
    public ResponseEntity<List<ReportInventoryItemDTO>> getInventoryReport() {
        return ResponseEntity.ok(reportService.getInventoryReport());
    }

    @GetMapping("/expiry")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','VIEWER','USER')")
    public ResponseEntity<ReportExpiryDTO> getExpiryReport() {
        return ResponseEntity.ok(reportService.getExpiryReport());
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','VIEWER','USER')")
    public ResponseEntity<List<ReportInventoryItemDTO>> getLowStockReport() {
        return ResponseEntity.ok(reportService.getLowStockReport());
    }

    @GetMapping("/{type}/export")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    public ResponseEntity<byte[]> exportReport(
            @PathVariable String type,
            @RequestParam(defaultValue = "csv") String format) {
        ReportService.ReportExport export = reportService.exportReport(type, format);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(export.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(export.filename())
                        .build()
                        .toString())
                .body(export.content());
    }
}
