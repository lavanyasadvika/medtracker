package com.medtracker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.openfda")
public class OpenFdaProperties {

    private String baseUrl = "https://api.fda.gov";
    private Integer timeoutSeconds = 10;
}
