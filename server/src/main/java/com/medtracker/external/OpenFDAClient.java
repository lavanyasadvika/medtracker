package com.medtracker.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.medtracker.config.OpenFdaProperties;
import com.medtracker.dto.ScanResultDTO;
import com.medtracker.entity.Source;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
public class OpenFDAClient {

    private static final int NOT_FOUND_STATUS = 404;

    private final RestClient restClient;

    public OpenFDAClient(RestClient.Builder restClientBuilder, OpenFdaProperties properties) {
        // Injecting Spring Boot's auto-configured builder (instead of RestClient.builder())
        // ensures calls are instrumented as child spans in the request trace.
        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory(properties.getTimeoutSeconds()))
                .build();
    }

    public Optional<ScanResultDTO> lookupByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return Optional.empty();
        }

        String uri = UriComponentsBuilder.fromPath("/drug/ndc.json")
                .queryParam("search", "product_ndc:\"" + barcode + "\" OR package_ndc:\"" + barcode + "\"")
                .queryParam("limit", 1)
                .build()
                .encode()
                .toUriString();

        try {
            JsonNode response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(JsonNode.class);
            return parseOpenFdaResponse(response, barcode);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == NOT_FOUND_STATUS) {
                return Optional.empty();
            }
            log.warn("OpenFDA lookup failed for barcode {} with status {}", barcode, ex.getStatusCode().value());
            return Optional.empty();
        } catch (RestClientException ex) {
            log.warn("OpenFDA lookup failed for barcode {}: {}", barcode, ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<ScanResultDTO> parseOpenFdaResponse(JsonNode response, String barcode) {
        if (response == null || !response.path("results").isArray() || response.path("results").isEmpty()) {
            return Optional.empty();
        }

        JsonNode result = response.path("results").get(0);
        String drugName = firstNonBlank(result.path("brand_name").asText(null), result.path("generic_name").asText(null));
        if (drugName == null) {
            return Optional.empty();
        }

        return Optional.of(ScanResultDTO.builder()
                .drugName(drugName)
                .manufacturer(emptyToNull(result.path("labeler_name").asText(null)))
                .barcode(barcode)
                .source(Source.BARCODE)
                .manualEntryRequired(Boolean.FALSE)
                .message("Barcode matched using OpenFDA.")
                .build());
    }

    private String firstNonBlank(String first, String second) {
        String normalizedFirst = emptyToNull(first);
        return normalizedFirst == null ? emptyToNull(second) : normalizedFirst;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private SimpleClientHttpRequestFactory requestFactory(Integer timeoutSeconds) {
        int timeoutMillis = Math.toIntExact(Duration.ofSeconds(timeoutSeconds == null ? 10L : timeoutSeconds).toMillis());
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);
        return factory;
    }
}
