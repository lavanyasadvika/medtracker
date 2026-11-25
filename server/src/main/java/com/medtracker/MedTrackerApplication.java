package com.medtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MedTracker – Medicine Inventory and Expiry Tracker
 *
 * <p>Entry point for the Spring Boot 3 application.
 * Flyway runs migrations automatically on startup before Hibernate validates the schema.
 *
 * @author Sunil Regar
 */
@SpringBootApplication
public class MedTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedTrackerApplication.class, args);
    }
}
