package com.adren.travel.booking;

import com.adren.travel.infra.TestInfrastructure;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full end-to-end integration test: real Spring Boot context, real
 * Postgres (via Testcontainers), real LocalStack (via Testcontainers) — the
 * slowest and most thorough tier in the test pyramid described in the
 * {@code testing-strategy} skill. Run via {@code ./gradlew integrationTest},
 * kept separate from the fast {@code ./gradlew test} suite (unit tests +
 * @ApplicationModuleTest slices) so local dev loops stay quick.
 * <p>
 * Requires Docker to be available on the host/CI runner.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = TestInfrastructure.class)
class BookingEndToEndIT {

    @Autowired
    BookingApi bookingApi;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void confirmBookingEndToEndAgainstRealDatabaseAndLocalStack() {
        authenticateAsSuperAdmin();
        Money price = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);
        UUID quotationId = insertQuotationForANewDraftItinerary();

        UUID bookingId = bookingApi.confirmBooking(quotationId, price);

        assertThat(bookingId).isNotNull();
        // A fuller test would also assert that the BookingConfirmedEvent
        // listener published a message to the LocalStack SNS topic backing
        // notifications (PRD Section 15) — inject an SNS client pointed at
        // TestInfrastructure.LOCALSTACK and poll the queue once the
        // Notification module's real SNS publishing code exists.
    }

    // BOK-13: confirmBooking now resolves a real consultantId from the
    // quotation/package it's given, so this test needs a genuinely
    // persisted Quotation (+ its Itinerary) rather than a random UUID.
    private UUID insertQuotationForANewDraftItinerary() {
        UUID itineraryId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO itinerary (itinerary_id, consultant_id, status, ai_generated, created_at, updated_at) " +
                "VALUES (?, ?, 'DRAFT', false, now(), now())",
            itineraryId, UUID.randomUUID());
        UUID quotationId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO quotation (quotation_id, itinerary_id, valid_until, shared_with_traveler, created_at) " +
                "VALUES (?, ?, now() + interval '7 days', false, now())",
            quotationId, itineraryId);
        return quotationId;
    }

    private static void authenticateAsSuperAdmin() {
        AdrenPrincipal principal = new AdrenPrincipal(UUID.randomUUID(), Role.SUPER_ADMIN, null);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
