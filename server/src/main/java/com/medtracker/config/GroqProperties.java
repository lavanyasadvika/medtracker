package com.medtracker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.groq")
public class GroqProperties {

    private String apiKey;
    private String baseUrl = "https://api.groq.com/openai/v1";
    private String model = "meta-llama/llama-4-scout-17b-16e-instruct";
    private Integer timeoutSeconds = 30;
    private Boolean mockEnabled = Boolean.FALSE;
}
