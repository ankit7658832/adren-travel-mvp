package com.adren.travel.payments;

import com.adren.travel.shared.Money;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

/**
 * Public API of the Payments, Yield/Markup &amp; Wallet module. Other
 * modules must depend on this interface, never on classes under
 * {@code com.adren.travel.payments.internal}.
 */
public interface PaymentsApi {

    /**
     * Configures a Consultant's markup rule for one product category (PRD
     * §12.1). PRD §3.3 — a Consultant's own Users "cannot change markup...
     * unless granted", so this is {@code CONSULTANT}-only (self-scoped to
     * the caller's own account) rather than the {@code CONSULTANT,USER}
     * shape most of {@code BookingApi} uses; {@code SUPER_ADMIN} retains
     * the usual oversight path. {@code consultantId} is checked against
     * the caller's own tenant via {@code CurrentPrincipal.resolveTenantScope}
     * (RULES.md §5.2), the same as every other tenant-scoped lookup.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT')")
    void configureMarkup(UUID consultantId, ConfigureMarkupCommand command);

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT')")
    List<MarkupRuleView> findMarkupRules(UUID consultantId);

    /**
     * Returns a Consultant's wallet — available balance, credit limit, and
     * pending holds, denominated in the home-market currency (PRD §12.3,
     * FIN-06). Unlike {@code configureMarkup}, this is a read with no
     * financial side effect for the caller, so {@code USER} is included
     * (a Consultant's Users can see wallet status even though they cannot
     * change markup, per PRD §3.3). {@code consultantId} is checked against
     * the caller's own tenant via {@code CurrentPrincipal.resolveTenantScope}.
     * A wallet is auto-provisioned with zero balance/credit-limit on first
     * access — there is no separate "create wallet" story.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    WalletView getWallet(UUID consultantId);

    /**
     * Calculates Adren's commission on a booking's supplier net rate,
     * kept separate from the Consultant's own markup (PRD §12.1 Worked
     * Example A, FIN-02). Invoked internally as part of sell-rate
     * calculation (FIN-05), not exposed as its own user-facing REST
     * action — no {@code @PreAuthorize} here, the same shape as
     * {@code WhitelabelApi#requireConsultantActive}: it's consulted
     * mid-flow by an already-authorized caller, not invoked directly.
     */
    Money calculateCommission(CalculateCommissionCommand command);

    /**
     * Applies a Consultant/market's currency buffer (2-5%) to an
     * FX-converted base rate, before markup (PRD §12.2, §12.1 Worked
     * Example B, FIN-03). Same internal-pricing-pipeline-step shape as
     * {@link #calculateCommission} — no {@code @PreAuthorize}.
     */
    Money applyCurrencyBuffer(ApplyCurrencyBufferCommand command);

    /**
     * Captures and locks a booking's FX rate at quotation time (PRD §12.2,
     * §22.4 T7, FIN-04). Same internal-pricing-pipeline-step shape as
     * {@link #calculateCommission}/{@link #applyCurrencyBuffer} — no
     * {@code @PreAuthorize}. The returned {@link FxRateSnapshot} is what
     * every later calculation on this booking must reuse; nothing in this
     * module re-fetches or recomputes it afterward.
     */
    FxRateSnapshot snapshotFxRate(SnapshotFxRateCommand command);

    /**
     * Runs the full net→buffer→markup→commission pipeline for one line
     * item (PRD §12.1 Worked Examples A &amp; B, FIN-05), composing
     * {@link #snapshotFxRate}, {@link #applyCurrencyBuffer}, the
     * Consultant's configured markup (FIN-01), and {@link
     * #calculateCommission} in order. Same internal-pricing-pipeline-step
     * shape as the steps it composes — no {@code @PreAuthorize}.
     */
    SellRateCalculation calculateSellRate(CalculateSellRateCommand command);

    /**
     * Creates a Stripe PaymentIntent for a booking (PRD §12.4, §24.4,
     * FIN-11). {@code consultantId} is checked against the caller's own
     * tenant via {@code CurrentPrincipal.resolveTenantScope}, matching
     * every other tenant-scoped write. {@code USER} is included since
     * paying for a booking is the same "make a booking" action PRD §6
     * grants Users.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    PaymentIntentView createPaymentIntent(CreatePaymentIntentCommand command);

    /**
     * Handles a Stripe webhook event (PRD §12.4, FIN-11) — not
     * {@code @PreAuthorize}-gated since the caller is Stripe itself, not
     * an authenticated Adren principal (a real deployment authenticates
     * this call via the {@code Stripe-Signature} header, verified in front
     * of this method — see {@link HandleStripeWebhookCommand}'s Javadoc).
     * On {@code "payment_intent.succeeded"}, publishes {@link
     * com.adren.travel.payments.event.StripePaymentSucceededEvent}, which
     * the Booking module listens for to gate {@code confirmBooking} on
     * webhook receipt rather than confirming on submission alone.
     */
    void handleStripeWebhook(HandleStripeWebhookCommand command);
}
