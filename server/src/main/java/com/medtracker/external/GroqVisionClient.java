package com.medtracker.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medtracker.config.GroqProperties;
import com.medtracker.dto.ScanResultDTO;
import com.medtracker.entity.Source;
import com.medtracker.exception.ScanExtractionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GroqVisionClient {

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
    private static final String PROMPT = "Extract the medicine drug name and expiry date from this label image. "
            + "Return ONLY JSON in this exact shape: {\"drug_name\":\"...\",\"expiry_date\":\"MM/YYYY\"}. "
            + "Do not include markdown, explanations, code fences, or any extra fields.";
    private static final DateTimeFormatter EXPIRY_FORMATTER = DateTimeFormatter.ofPattern("MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private final ObjectMapper objectMapper;
    private final GroqProperties properties;
    private final RestClient restClient;

    public GroqVisionClient(ObjectMapper objectMapper, RestClient.Builder restClientBuilder, GroqProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        // Injecting Spring Boot's auto-configured builder (instead of RestClient.builder())
        // ensures calls are instrumented as child spans in the request trace.
        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory(properties.getTimeoutSeconds()))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public ScanResultDTO extract(byte[] image) {
        if (Boolean.TRUE.equals(properties.getMockEnabled())) {
            return mockedExtractionResult();
        }
        if (image == null || image.length == 0) {
            throw new ScanExtractionException("Image is required for Groq extraction.");
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()
                || properties.getApiKey().startsWith("REPLACE_WITH")) {
            throw new ScanExtractionException("Groq API key is not configured.");
        }

        try {
            JsonNode response = restClient.post()
                    .uri(CHAT_COMPLETIONS_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .body(buildRequestBody(image))
                    .retrieve()
                    .body(JsonNode.class);
            return parseResponse(response);
        } catch (RestClientException ex) {
            log.warn("Groq Vision extraction request failed: {}", ex.getMessage(), ex);
            throw new ScanExtractionException("Groq Vision extraction failed.", ex);
        }
    }

    private Map<String, Object> buildRequestBody(byte[] image) {
        String base64Image = Base64.getEncoder().encodeToString(image);
        return Map.of(
                "model", properties.getModel(),
                "temperature", 0,
                "max_tokens", 200,
                // qwen/qwen3.6-27b defaults to "thinking mode" and wraps its answer in a
                // <think>...</think> preamble, which breaks the strict JSON parse below.
                // "none" returns just the final answer. Revisit if the model is changed.
                "reasoning_effort", "none",
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "text", "text", PROMPT),
                                Map.of("type", "image_url", "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image))
                        )
                ))
        );
    }

    private ScanResultDTO parseResponse(JsonNode response) {
        String content = response == null ? null : response.path("choices").path(0).path("message").path("content").asText(null);
        if (content == null || content.isBlank()) {
            throw new ScanExtractionException("Groq Vision response did not contain extractable JSON.");
        }

        try {
            JsonNode extractedJson = objectMapper.readTree(content);
            String drugName = requiredText(extractedJson, "drug_name");
            String expiry = requiredText(extractedJson, "expiry_date");
            LocalDate expiryDate = YearMonth.parse(expiry, EXPIRY_FORMATTER).atEndOfMonth();

            return ScanResultDTO.builder()
                    .drugName(drugName)
                    .expiryDate(expiryDate)
                    .source(Source.GROQ)
                    .manualEntryRequired(Boolean.FALSE)
                    .message("Label extracted using Groq Vision.")
                    .build();
        } catch (JsonProcessingException | DateTimeParseException | IllegalArgumentException ex) {
            log.warn("Groq Vision returned content that could not be parsed as strict medicine JSON: {}", content, ex);
            throw new ScanExtractionException("Groq Vision response could not be parsed as strict medicine JSON.", ex);
        }
    }

    private String requiredText(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || node.path(fieldName).asText().isBlank()) {
            throw new IllegalArgumentException("Missing required Groq field: " + fieldName);
        }
        return node.path(fieldName).asText();
    }

    private ScanResultDTO mockedExtractionResult() {
        return ScanResultDTO.builder()
                .drugName("Mock Medicine")
                .expiryDate(YearMonth.now().plusYears(1).atEndOfMonth())
                .source(Source.GROQ)
                .manualEntryRequired(Boolean.FALSE)
                .message("Mock Groq extraction enabled for offline testing.")
                .build();
    }

    private SimpleClientHttpRequestFactory requestFactory(Integer timeoutSeconds) {
        int timeoutMillis = Math.toIntExact(Duration.ofSeconds(timeoutSeconds == null ? 30L : timeoutSeconds).toMillis());
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);
        return factory;
    }
}
