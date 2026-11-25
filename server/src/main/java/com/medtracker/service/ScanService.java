package com.medtracker.service;

import com.medtracker.dto.ScanResultDTO;
import com.medtracker.entity.Source;
import com.medtracker.exception.ScanExtractionException;
import com.medtracker.external.GroqVisionClient;
import com.medtracker.external.OpenFDAClient;
import com.medtracker.external.ZXingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScanService {

    private static final String RETAKE_OR_MANUAL_MESSAGE = "Unable to extract medicine details from the image. Please retake the photo or enter details manually.";

    private final ZXingService zXingService;
    private final OpenFDAClient openFDAClient;
    private final GroqVisionClient groqVisionClient;

    public ScanResultDTO scan(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Scan image is required.");
        }

        byte[] imageBytes = readImage(image);
        return zXingService.decode(imageBytes)
                .map(this::scanBarcodePath)
                .orElseGet(() -> scanGroqPath(imageBytes));
    }

    private ScanResultDTO scanBarcodePath(String barcode) {
        return openFDAClient.lookupByBarcode(barcode)
                .map(result -> applyBarcodeDefaults(result, barcode))
                .orElseGet(() -> buildManualEntryResult(barcode));
    }

    private ScanResultDTO scanGroqPath(byte[] imageBytes) {
        try {
            ScanResultDTO result = groqVisionClient.extract(imageBytes);
            result.setSource(Source.GROQ);
            result.setManualEntryRequired(Boolean.FALSE);
            return result;
        } catch (ScanExtractionException ex) {
            log.warn("Groq extraction failed: {}", ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, RETAKE_OR_MANUAL_MESSAGE, ex);
        }
    }

    private ScanResultDTO applyBarcodeDefaults(ScanResultDTO result, String barcode) {
        result.setBarcode(barcode);
        result.setSource(Source.BARCODE);
        result.setManualEntryRequired(Boolean.FALSE);
        if (result.getMessage() == null || result.getMessage().isBlank()) {
            result.setMessage("Barcode matched using OpenFDA.");
        }
        return result;
    }

    private ScanResultDTO buildManualEntryResult(String barcode) {
        return ScanResultDTO.builder()
                .barcode(barcode)
                .source(Source.BARCODE)
                .manualEntryRequired(Boolean.TRUE)
                .message("Barcode was detected, but OpenFDA did not return a matching medicine. Please complete the details manually.")
                .build();
    }

    private byte[] readImage(MultipartFile image) {
        try {
            return image.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read uploaded scan image.", ex);
        }
    }
}
