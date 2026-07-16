package com.adren.travel.booking;

import com.adren.travel.infra.TestInfrastructure;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BOK-16's actual acceptance criterion, proven under real concurrency (not
 * just asserted by a mocked-exception unit test — see {@code
 * BookingServiceImplTest#confirmBookingMapsALostOptimisticLockRaceToInventoryNoLongerAvailableBOK16}
 * for that tier): N threads race to confirm the SAME quotation; exactly one
 * must succeed, every other must fail with {@link
 * InventoryNoLongerAvailableException}, never a duplicate booking.
 * <p>
 * Requires Docker to be available on the host/CI runner — see {@code
 * WalletLedgerConcurrentWriteIT} for the same tier/shape.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(initializers = TestInfrastructure.class)
class BookingConcurrentConfirmationIT {

    private static final int CONCURRENT_CONFIRMERS = 8;

    @Autowired
    BookingApi bookingApi;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void onlyOneOfManyConcurrentConfirmationsOfTheSameQuotationSucceeds() throws InterruptedException {
        UUID quotationId = insertQuotationForAQuotationStatusItinerary();
        Money price = new Money(BigDecimal.valueOf(1_000), CurrencyCode.INR);

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_CONFIRMERS);
        CountDownLatch readyLatch = new CountDownLatch(CONCURRENT_CONFIRMERS);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger noLongerAvailableCount = new AtomicInteger();
        try {
            List<Callable<Void>> tasks = new java.util.ArrayList<>();
            for (int i = 0; i < CONCURRENT_CONFIRMERS; i++) {
                tasks.add(() -> {
                    authenticateAsSuperAdmin();
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                        bookingApi.confirmBooking(quotationId, price);
                        successCount.incrementAndGet();
                    } catch (InventoryNoLongerAvailableException e) {
                        noLongerAvailableCount.incrementAndGet();
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                    return null;
                });
            }
            List<Future<Void>> futures = new java.util.ArrayList<>();
            for (Callable<Void> task : tasks) {
                futures.add(executor.submit(task));
            }
            readyLatch.await(10, TimeUnit.SECONDS);
            startLatch.countDown(); // release all confirmers at once
            executor.shutdown();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
            for (Future<Void> future : futures) {
                future.get(); // surface any unexpected exception the test didn't anticipate
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            executor.shutdownNow();
        }

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(noLongerAvailableCount.get()).isEqualTo(CONCURRENT_CONFIRMERS - 1);

        String finalStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM itinerary i JOIN quotation q ON q.itinerary_id = i.itinerary_id WHERE q.quotation_id = ?",
            String.class, quotationId);
        assertThat(finalStatus).isEqualTo("BOOKED");
    }

    private UUID insertQuotationForAQuotationStatusItinerary() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO itinerary (itinerary_id, consultant_id, status, ai_generated, created_at, updated_at) " +
                "VALUES (?, ?, 'QUOTATION', false, now(), now())",
            itineraryId, consultantId);
        // FIN-08: confirmBooking's wallet path now enforces the credit
        // limit — whichever of the N concurrent confirmers wins the
        // optimistic-lock race still needs a funded wallet to complete.
        jdbcTemplate.update(
            "INSERT INTO wallet (consultant_id, available_balance, credit_limit, pending_holds, currency, updated_at) " +
                "VALUES (?, 0, 100000, 0, 'INR', now())",
            consultantId);
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
