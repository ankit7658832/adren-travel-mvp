package com.adren.travel.booking;

import com.adren.travel.infra.TestInfrastructure;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
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

    @Test
    void confirmBookingEndToEndAgainstRealDatabaseAndLocalStack() {
        Money price = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);

        UUID bookingId = bookingApi.confirmBooking(UUID.randomUUID(), price);

        assertThat(bookingId).isNotNull();
        // A fuller test would also assert that the BookingConfirmedEvent
        // listener published a message to the LocalStack SNS topic backing
        // notifications (PRD Section 15) — inject an SNS client pointed at
        // TestInfrastructure.LOCALSTACK and poll the queue once the
        // Notification module's real SNS publishing code exists.
    }
}
