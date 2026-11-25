package com.medtracker.service;

import com.medtracker.entity.Medicine;
import com.medtracker.entity.Role;
import com.medtracker.entity.User;
import com.medtracker.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final Set<Role> ALERT_RECIPIENT_ROLES = Set.of(Role.ADMIN, Role.PHARMACIST);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final JavaMailSender javaMailSender;
    private final UserRepository userRepository;

    @Value("${app.mail.from:noreply@medtracker.local}")
    private String fromAddress;

    @Value("${app.mail.from-name:MedTracker}")
    private String fromName;

    public void sendExpiryAlert(List<Medicine> expiringSoon, List<Medicine> expired) {
        sendHtmlAlert(
                "MedTracker expiry alert",
                buildExpiryAlertHtml(expiringSoon, expired)
        );
    }

    public void sendLowStockAlert(List<Medicine> lowStock) {
        sendHtmlAlert(
                "MedTracker low-stock alert",
                buildMedicineTableEmail(
                        "Low-stock medicines",
                        "The following medicines are at or below their configured low-stock threshold.",
                        lowStock)
        );
    }

    private void sendHtmlAlert(String subject, String htmlBody) {
        List<String> recipients = findAlertRecipients();
        if (recipients.isEmpty()) {
            log.warn("Skipping alert email '{}' because no active ADMIN or PHARMACIST recipients were found", subject);
            return;
        }

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    false,
                    StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress, fromName);
            helper.setTo(recipients.toArray(String[]::new));
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            javaMailSender.send(mimeMessage);
            log.info("Sent alert email '{}' to {} recipients", subject, recipients.size());
        } catch (MessagingException | MailException ex) {
            log.error("Failed to send alert email '{}': {}", subject, ex.getMessage());
        } catch (java.io.UnsupportedEncodingException ex) {
            log.error("Failed to apply alert email sender name '{}': {}", fromName, ex.getMessage());
        }
    }

    private List<String> findAlertRecipients() {
        return userRepository.findByIsActiveTrueAndRoleIn(ALERT_RECIPIENT_ROLES)
                .stream()
                .map(User::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .distinct()
                .toList();
    }

    private String buildExpiryAlertHtml(List<Medicine> expiringSoon, List<Medicine> expired) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>")
                .append("<h2>Medicine expiry alert</h2>")
                .append("<p>Please review medicines that are expired or expiring soon.</p>");
        appendMedicineSection(html, "Expired medicines", expired);
        appendMedicineSection(html, "Expiring soon", expiringSoon);
        html.append("</body></html>");
        return html.toString();
    }

    private String buildMedicineTableEmail(String title, String message, List<Medicine> medicines) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>")
                .append("<h2>").append(escapeHtml(title)).append("</h2>")
                .append("<p>").append(escapeHtml(message)).append("</p>");
        appendMedicineSection(html, title, medicines);
        html.append("</body></html>");
        return html.toString();
    }

    private void appendMedicineSection(StringBuilder html, String title, Collection<Medicine> medicines) {
        html.append("<h3>").append(escapeHtml(title)).append("</h3>");
        if (medicines == null || medicines.isEmpty()) {
            html.append("<p>No medicines in this category.</p>");
            return;
        }

        html.append("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">")
                .append("<thead><tr>")
                .append("<th>Name</th><th>Manufacturer</th><th>Batch</th><th>Expiry</th><th>Quantity</th><th>Low-stock threshold</th>")
                .append("</tr></thead><tbody>");

        for (Medicine medicine : medicines) {
            html.append("<tr>")
                    .append("<td>").append(escapeHtml(medicine.getDrugName())).append("</td>")
                    .append("<td>").append(escapeHtml(medicine.getManufacturer())).append("</td>")
                    .append("<td>").append(escapeHtml(medicine.getBatchNumber())).append("</td>")
                    .append("<td>").append(medicine.getExpiryDate() == null ? "" : DATE_FORMATTER.format(medicine.getExpiryDate())).append("</td>")
                    .append("<td>").append(medicine.getQuantity() == null ? "" : medicine.getQuantity().toString()).append("</td>")
                    .append("<td>").append(medicine.getLowStockThreshold() == null ? "" : medicine.getLowStockThreshold().toString()).append("</td>")
                    .append("</tr>");
        }

        html.append("</tbody></table>");
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
