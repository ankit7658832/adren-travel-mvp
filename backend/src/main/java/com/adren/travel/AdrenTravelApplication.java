package com.adren.travel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

/**
 * ADREN TRAVEL — B2B Travel Booking Platform.
 *
 * This application is structured as a Spring Modulith modular monolith. Each
 * top-level package under {@code com.adren.travel} (booking, supplier, ai,
 * payments, whitelabel, ads, notification, compliance, shared) is an
 * application module per the PRD's module boundaries (PRD Sections 9-17).
 *
 * Module boundaries are verified automatically by
 * {@code ModularityTests} (see src/test) using
 * {@code ApplicationModules.of(AdrenTravelApplication.class).verify()}.
 *
 * Cross-module communication MUST go through Spring's ApplicationEventPublisher
 * (domain events) or a module's public API package — never by reaching into
 * another module's internal classes directly. See the
 * `backend-spring-modulith` Claude Code skill for the full convention.
 */
@SpringBootApplication
@Modulithic(systemName = "ADREN TRAVEL")
public class AdrenTravelApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdrenTravelApplication.class, args);
    }
}
