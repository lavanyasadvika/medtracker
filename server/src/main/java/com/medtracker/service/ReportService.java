package com.medtracker.service;

import com.medtracker.config.AlertProperties;
import com.medtracker.dto.ReportExpiryDTO;
import com.medtracker.dto.ReportExpiryItemDTO;
import com.medtracker.dto.ReportInventoryItemDTO;
import com.medtracker.entity.Medicine;
import com.medtracker.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String CSV_CONTENT_TYPE = "text/csv";
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String INVENTORY_REPORT = "inventory";
    private static final String EXPIRY_REPORT = "expiry";
    private static final String LOW_STOCK_REPORT = "low-stock";

    private final MedicineRepository medicineRepository;
    private final AlertProperties alertProperties;

    @Transactional(readOnly = true)
    public List<ReportInventoryItemDTO> getInventoryReport() {
        return medicineRepository.findByIsActiveTrueOrderByDrugNameAsc()
                .stream()
                .map(this::toInventoryItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReportExpiryDTO getExpiryReport() {
        LocalDate today = LocalDate.now();
        LocalDate leadDate = today.plusDays(alertProperties.getExpiryLeadDays());
        List<Medicine> activeMedicines = medicineRepository.findByIsActiveTrueOrderByDrugNameAsc();

        return ReportExpiryDTO.builder()
                .expired(filterByExpiryStatus(activeMedicines, today, leadDate, "red"))
                .expiring(filterByExpiryStatus(activeMedicines, today, leadDate, "amber"))
                .safe(filterByExpiryStatus(activeMedicines, today, leadDate, "green"))
                .build();
    }

    @Transactional(readOnly = true)
    public List<ReportInventoryItemDTO> getLowStockReport() {
        Integer defaultThreshold = alertProperties.getDefaultLowStockThreshold();
        return medicineRepository.findLowStock(defaultThreshold)
                .stream()
                .map(this::toInventoryItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReportExport exportReport(String type, String format) {
        String normalizedType = normalizeType(type);
        String normalizedFormat = normalizeFormat(format);
        List<List<String>> rows = buildRows(normalizedType);
        String filename = buildFilename(normalizedType, normalizedFormat);

        if ("csv".equals(normalizedFormat)) {
            return new ReportExport(filename, CSV_CONTENT_TYPE, writeCsv(rows));
        }

        return new ReportExport(filename, PDF_CONTENT_TYPE, writePdf(titleFor(normalizedType), rows));
    }

    private List<ReportExpiryItemDTO> filterByExpiryStatus(
            List<Medicine> medicines,
            LocalDate today,
            LocalDate leadDate,
            String status) {
        return medicines.stream()
                .filter(medicine -> matchesStatus(medicine.getExpiryDate(), today, leadDate, status))
                .map(medicine -> toExpiryItem(medicine, status))
                .toList();
    }

    private boolean matchesStatus(LocalDate expiryDate, LocalDate today, LocalDate leadDate, String status) {
        if ("red".equals(status)) {
            return expiryDate.isBefore(today);
        }
        if ("amber".equals(status)) {
            return !expiryDate.isBefore(today) && !expiryDate.isAfter(leadDate);
        }
        return expiryDate.isAfter(leadDate);
    }

    private List<List<String>> buildRows(String normalizedType) {
        if (INVENTORY_REPORT.equals(normalizedType)) {
            return inventoryRows(getInventoryReport());
        }
        if (EXPIRY_REPORT.equals(normalizedType)) {
            return expiryRows(getExpiryReport());
        }
        if (LOW_STOCK_REPORT.equals(normalizedType)) {
            return inventoryRows(getLowStockReport());
        }
        throw new IllegalArgumentException("Unsupported report type: " + normalizedType);
    }

    private List<List<String>> inventoryRows(List<ReportInventoryItemDTO> reportItems) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Name", "Category", "Quantity", "Expiry", "Manufacturer"));
        reportItems.forEach(item -> rows.add(List.of(
                safe(item.getName()),
                safe(item.getCategory()),
                formatQuantity(item.getQuantity()),
                formatDate(item.getExpiry()),
                safe(item.getManufacturer()))));
        return rows;
    }

    private List<List<String>> expiryRows(ReportExpiryDTO report) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Bucket", "Name", "Category", "Quantity", "Expiry", "Manufacturer", "Status"));
        addExpiryRows(rows, "expired", report.getExpired());
        addExpiryRows(rows, "expiring", report.getExpiring());
        addExpiryRows(rows, "safe", report.getSafe());
        return rows;
    }

    private void addExpiryRows(List<List<String>> rows, String bucket, List<ReportExpiryItemDTO> items) {
        items.forEach(item -> rows.add(List.of(
                bucket,
                safe(item.getName()),
                safe(item.getCategory()),
                formatQuantity(item.getQuantity()),
                formatDate(item.getExpiry()),
                safe(item.getManufacturer()),
                safe(item.getStatus()))));
    }

    private byte[] writeCsv(List<List<String>> rows) {
        StringBuilder csv = new StringBuilder();
        rows.forEach(row -> csv.append(row.stream()
                        .map(this::escapeCsv)
                        .reduce((left, right) -> left + "," + right)
                        .orElse(""))
                .append(System.lineSeparator()));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] writePdf(String title, List<List<String>> rows) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float yPosition = 740;
                contentStream.beginText();
                contentStream.setFont(boldFont, 14);
                contentStream.newLineAtOffset(50, yPosition);
                contentStream.showText(title);
                contentStream.endText();

                yPosition -= 28;
                for (List<String> row : rows) {
                    if (yPosition < 50) {
                        break;
                    }
                    contentStream.beginText();
                    contentStream.setFont(row == rows.get(0) ? boldFont : font, 9);
                    contentStream.newLineAtOffset(50, yPosition);
                    contentStream.showText(toPdfLine(row));
                    contentStream.endText();
                    yPosition -= 16;
                }
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to generate PDF report", ex);
        }
    }

    private ReportInventoryItemDTO toInventoryItem(Medicine medicine) {
        return ReportInventoryItemDTO.builder()
                .name(medicine.getDrugName())
                .category(medicine.getCategory())
                .quantity(medicine.getQuantity())
                .expiry(medicine.getExpiryDate())
                .manufacturer(medicine.getManufacturer())
                .build();
    }

    private ReportExpiryItemDTO toExpiryItem(Medicine medicine, String status) {
        return ReportExpiryItemDTO.builder()
                .name(medicine.getDrugName())
                .category(medicine.getCategory())
                .quantity(medicine.getQuantity())
                .expiry(medicine.getExpiryDate())
                .manufacturer(medicine.getManufacturer())
                .status(status)
                .build();
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Report type is required");
        }
        return type.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeFormat(String format) {
        if (format == null || format.isBlank()) {
            return "csv";
        }
        String normalizedFormat = format.trim().toLowerCase(Locale.ROOT);
        if (!"csv".equals(normalizedFormat) && !"pdf".equals(normalizedFormat)) {
            throw new IllegalArgumentException("Report export format must be csv or pdf");
        }
        return normalizedFormat;
    }

    private String buildFilename(String type, String format) {
        return "medtracker-" + type + "-report-" + LocalDate.now() + "." + format;
    }

    private String titleFor(String type) {
        return "MedTracker " + type.replace('-', ' ') + " report";
    }

    private String escapeCsv(String value) {
        String safeValue = safe(value);
        if (safeValue.contains(",") || safeValue.contains("\"") || safeValue.contains("\n") || safeValue.contains("\r")) {
            return "\"" + safeValue.replace("\"", "\"\"") + "\"";
        }
        return safeValue;
    }

    private String toPdfLine(List<String> row) {
        String line = String.join(" | ", row);
        return line.length() > 115 ? line.substring(0, 112) + "..." : line;
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : DATE_FORMATTER.format(date);
    }

    private String formatQuantity(Integer value) {
        return value == null ? "" : value.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record ReportExport(String filename, String contentType, byte[] content) {
    }
}
