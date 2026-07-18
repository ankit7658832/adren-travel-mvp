package com.adren.travel.supplier.internal.localdmc;

import com.adren.travel.supplier.LocalDmcVerificationRequiredException;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * A Local DMC (destination-management-company) partner onboarded manually,
 * no live API (PRD §10.3, §20.14, DMC-01/02) — package-private, own table.
 * Starts {@link LocalDmcStatus#PENDING}; {@link #activate} is the ONLY path
 * to {@link LocalDmcStatus#ACTIVE}, and throws rather than silently
 * allowing a transition with no recorded verification step
 * (backend-best-practices §1 — the state-transition rule lives on the
 * entity that owns the state, not scattered across call sites).
 */
@Entity
@Table(name = "local_dmc_record")
public class LocalDmcRecord {

    @Id
    private UUID localDmcId;

    private UUID consultantId;
    private String businessName;
    private String productCategories;
    private String sampleRatesSummary;
    private String referencesInfo;

    @Enumerated(EnumType.STRING)
    private LocalDmcStatus status;

    private String verificationNotes;

    private int totalBookingsCount;
    private int cancelledBookingsCount;
    private BigDecimal cancellationRate;
    private int complaintCount;
    private boolean flagged;
    private boolean inventoryStale;

    private Instant createdAt;

    protected LocalDmcRecord() {
        // JPA
    }

    public LocalDmcRecord(UUID localDmcId, UUID consultantId, String businessName, String productCategories,
                           String sampleRatesSummary, String referencesInfo) {
        this.localDmcId = localDmcId;
        this.consultantId = consultantId;
        this.businessName = businessName;
        this.productCategories = productCategories;
        this.sampleRatesSummary = sampleRatesSummary;
        this.referencesInfo = referencesInfo;
        // DMC-01 AC: Pending, not Active, until at least one verification
        // step completes — the load-bearing invariant, enforced here at
        // construction, never left to a caller to remember to set.
        this.status = LocalDmcStatus.PENDING;
        this.totalBookingsCount = 0;
        this.cancelledBookingsCount = 0;
        this.cancellationRate = BigDecimal.ZERO;
        this.complaintCount = 0;
        this.flagged = false;
        this.inventoryStale = false;
        this.createdAt = Instant.now();
    }

    /**
     * DMC-02: Pending → Active, gated on a non-blank verification note —
     * the single required "verification step" this scaffold models (no
     * separate multi-step checklist entity exists yet). Throws rather than
     * silently allowing the transition when the reviewer attempts to
     * activate without recording one.
     */
    public void activate(String verificationNotes) {
        if (status != LocalDmcStatus.PENDING) {
            throw new IllegalStateException("Local DMC " + localDmcId + " is not Pending (current status: " + status + ")");
        }
        if (verificationNotes == null || verificationNotes.isBlank()) {
            throw new LocalDmcVerificationRequiredException(localDmcId);
        }
        this.status = LocalDmcStatus.ACTIVE;
        this.verificationNotes = verificationNotes;
    }

    /**
     * DMC-04, PRD §10.3 step 5: recalculates the rolling cancellation rate
     * against this DMC's own booking/cancellation counters. Callable
     * directly (not yet wired to a live {@code booking}-published event —
     * see {@code LocalDmcService}'s Javadoc for why that wiring doesn't
     * exist yet in this codebase).
     */
    public void recordBooking() {
        this.totalBookingsCount++;
        recalculateCancellationRate();
    }

    public void recordCancellation(BigDecimal flagThreshold) {
        this.cancelledBookingsCount++;
        recalculateCancellationRate();
        // DMC-05: extends the SAME recalculation with a threshold check —
        // never trusts a caller-supplied "should this be flagged" claim,
        // always recomputed from the real counters.
        if (flagThreshold != null && cancellationRate.compareTo(flagThreshold) > 0) {
            this.flagged = true;
        }
    }

    public void recordComplaint(int complaintCountThreshold) {
        this.complaintCount++;
        if (complaintCount >= complaintCountThreshold) {
            this.flagged = true;
        }
    }

    private void recalculateCancellationRate() {
        this.cancellationRate = totalBookingsCount == 0
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(cancelledBookingsCount)
                .divide(BigDecimal.valueOf(totalBookingsCount), 4, RoundingMode.HALF_UP);
    }

    /** DMC-11: set/cleared by the scheduled staleness check — self-clears once inventory is refreshed. */
    void setInventoryStale(boolean inventoryStale) {
        this.inventoryStale = inventoryStale;
    }

    public UUID getLocalDmcId() {
        return localDmcId;
    }

    public UUID getConsultantId() {
        return consultantId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public String getProductCategories() {
        return productCategories;
    }

    public String getSampleRatesSummary() {
        return sampleRatesSummary;
    }

    public String getReferencesInfo() {
        return referencesInfo;
    }

    public LocalDmcStatus getStatus() {
        return status;
    }

    public String getVerificationNotes() {
        return verificationNotes;
    }

    public int getTotalBookingsCount() {
        return totalBookingsCount;
    }

    public int getCancelledBookingsCount() {
        return cancelledBookingsCount;
    }

    public BigDecimal getCancellationRate() {
        return cancellationRate;
    }

    public int getComplaintCount() {
        return complaintCount;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public boolean isInventoryStale() {
        return inventoryStale;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
