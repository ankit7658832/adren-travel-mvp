package com.adren.travel.payments;

import com.adren.travel.infra.TestInfrastructure;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FIN-10's actual acceptance criterion, proven under real concurrency: N
 * threads race to place the SAME wallet hold (same {@code bookingId});
 * only one may win, and the wallet's {@code pendingHolds} must reflect
 * exactly one hold, not N. A plain sequential retry (as in {@code
 * PaymentsModuleIntegrationTests#retryingAPlaceHoldForTheSameBookingIsANoOpFIN10})
 * only proves the fast-path {@code existsBy} check; this test is what
 * actually exercises {@code WalletLedgerEntryRecorder}'s {@code
 * REQUIRES_NEW} + unique-constraint-catch path, which only matters when
 * two writers are in flight at once. {@code placeHold} carries no {@code
 * @PreAuthorize} (an internal pricing/wallet-pipeline step, per its own
 * Javadoc), so no authenticated principal is needed to call it here.
 * <p>
 * Requires Docker to be available on the host/CI runner — see {@code
 * BookingEndToEndIT}/{@code PricingPipelineEndToEndIT} for the same
 * tier/shape.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = TestInfrastructure.class)
class WalletLedgerConcurrentWriteIT {

    private static final int CONCURRENT_WRITERS = 8;

    @Autowired
    PaymentsApi paymentsApi;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void onlyOneOfManyConcurrentPlaceHoldAttemptsForTheSameBookingSucceeds() throws InterruptedException {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Money amount = new Money(BigDecimal.valueOf(1_000), CurrencyCode.INR);
        var command = new WalletHoldCommand(bookingId, consultantId, amount);
        // FIN-08: placeHold now enforces the credit limit — all N writers
        // race for the SAME hold (idempotent no-op after the first), so one
        // funded wallet covers every attempt regardless of which succeeds.
        jdbcTemplate.update(
            "INSERT INTO wallet (consultant_id, available_balance, credit_limit, pending_holds, currency, updated_at) " +
                "VALUES (?, 0, 100000, 0, 'INR', now())",
            consultantId);

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_WRITERS);
        CountDownLatch readyLatch = new CountDownLatch(CONCURRENT_WRITERS);
        CountDownLatch startLatch = new CountDownLatch(1);
        try {
            for (int i = 0; i < CONCURRENT_WRITERS; i++) {
                executor.submit(() -> {
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                        paymentsApi.placeHold(command);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            readyLatch.await(10, TimeUnit.SECONDS);
            startLatch.countDown(); // release all writers at once
            executor.shutdown();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        Long holdEntryCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM wallet_ledger_entry WHERE related_booking_id = ? AND type = 'HOLD'",
            Long.class, bookingId);
        assertThat(holdEntryCount).isEqualTo(1);

        BigDecimal pendingHolds = jdbcTemplate.queryForObject(
            "SELECT pending_holds FROM wallet WHERE consultant_id = ?", BigDecimal.class, consultantId);
        assertThat(pendingHolds).isEqualByComparingTo("1000");
    }
}
