package com.adren.travel.supplier.internal.localdmc;

import com.adren.travel.supplier.LocalDmcVerificationRequiredException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalDmcRecordTest {

    @Test
    void aNewlySubmittedDmcStartsPendingNeverActiveFIN01() {
        LocalDmcRecord record = newRecord();

        assertThat(record.getStatus()).isEqualTo(LocalDmcStatus.PENDING);
    }

    @Test
    void activatingWithoutAVerificationNoteThrowsAndLeavesStatusPendingDMC02() {
        LocalDmcRecord record = newRecord();

        assertThatThrownBy(() -> record.activate(null))
            .isInstanceOf(LocalDmcVerificationRequiredException.class);
        assertThatThrownBy(() -> record.activate(""))
            .isInstanceOf(LocalDmcVerificationRequiredException.class);
        assertThatThrownBy(() -> record.activate("   "))
            .isInstanceOf(LocalDmcVerificationRequiredException.class);
        assertThat(record.getStatus()).isEqualTo(LocalDmcStatus.PENDING);
    }

    @Test
    void activatingWithAVerificationNoteTransitionsPendingToActiveDMC02() {
        LocalDmcRecord record = newRecord();

        record.activate("Business license and references checked by phone.");

        assertThat(record.getStatus()).isEqualTo(LocalDmcStatus.ACTIVE);
        assertThat(record.getVerificationNotes()).isEqualTo("Business license and references checked by phone.");
    }

    @Test
    void activatingAnAlreadyActiveDmcThrows() {
        LocalDmcRecord record = newRecord();
        record.activate("Checked.");

        assertThatThrownBy(() -> record.activate("Checked again."))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancellationRateRecalculatesAsARollingFigureDMC04() {
        LocalDmcRecord record = newRecord();

        record.recordBooking();
        record.recordBooking();
        record.recordBooking();
        record.recordBooking();
        record.recordCancellation(null);

        assertThat(record.getTotalBookingsCount()).isEqualTo(4);
        assertThat(record.getCancelledBookingsCount()).isEqualTo(1);
        assertThat(record.getCancellationRate()).isEqualByComparingTo(new BigDecimal("0.2500"));
    }

    @Test
    void cancellationRateStaysZeroWithNoBookingsYet() {
        LocalDmcRecord record = newRecord();

        assertThat(record.getCancellationRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void exceedingTheCancellationRateThresholdFlagsTheRecordDMC05() {
        LocalDmcRecord record = newRecord();
        record.recordBooking();
        record.recordBooking();

        // 1/2 = 0.50, threshold 0.20 -> exceeded.
        record.recordCancellation(new BigDecimal("0.20"));

        assertThat(record.isFlagged()).isTrue();
    }

    @Test
    void notExceedingTheCancellationRateThresholdLeavesTheRecordUnflaggedDMC05() {
        LocalDmcRecord record = newRecord();
        for (int i = 0; i < 9; i++) {
            record.recordBooking();
        }
        // 1/10 after this cancellation = 0.10, threshold 0.20 -> not exceeded.
        record.recordBooking();
        record.recordCancellation(new BigDecimal("0.20"));

        assertThat(record.isFlagged()).isFalse();
    }

    @Test
    void reachingTheComplaintCountThresholdFlagsTheRecordDMC05() {
        LocalDmcRecord record = newRecord();

        record.recordComplaint(3);
        assertThat(record.isFlagged()).isFalse();
        record.recordComplaint(3);
        assertThat(record.isFlagged()).isFalse();
        record.recordComplaint(3);

        assertThat(record.isFlagged()).isTrue();
        assertThat(record.getComplaintCount()).isEqualTo(3);
    }

    private static LocalDmcRecord newRecord() {
        return new LocalDmcRecord(UUID.randomUUID(), UUID.randomUUID(), "Goa Local Tours",
            "TRANSFER,ACTIVITY", "City tour from 2000 INR", "Ref: partner-hotel@example.com");
    }
}
