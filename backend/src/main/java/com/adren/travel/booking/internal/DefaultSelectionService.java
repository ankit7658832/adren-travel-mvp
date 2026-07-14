package com.adren.travel.booking.internal;

import com.adren.travel.supplier.SupplierId;
import com.adren.travel.supplier.SupplierSearchResult;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * PRD §9.2 / §22.2's four-step ranking, in strict tie-break order:
 * <ol>
 *   <li>Availability — must be confirmable in real time. The supplier
 *       aggregation layer only ever returns bookable rates today (no
 *       supplier client models an "unconfirmable" result), so every
 *       candidate passed in already satisfies this step by construction —
 *       still named explicitly here rather than silently assumed, so a
 *       future supplier client that CAN return unconfirmable rates has an
 *       obvious place to filter before calling this.</li>
 *   <li>The Consultant's configured preferred supplier, if any, wins
 *       outright over pure margin ranking (T3, PRD §22.2).</li>
 *   <li>Best margin among the (or non-preferred) remaining options. No
 *       markup/sell-rate pipeline exists yet (FIN-05, Financial Layer) —
 *       net rate is used as a placeholder proxy (lower cost ~ higher
 *       margin under a flat assumed markup) until that lands; swap the
 *       comparator's key then, not this method's shape.</li>
 *   <li>Rating as the final tiebreaker — missing ratings (no supplier
 *       content sync yet, PRD §10.5) sort last rather than failing.</li>
 * </ol>
 */
@Component
class DefaultSelectionService {

    Optional<SupplierSearchResult> selectDefault(List<SupplierSearchResult> confirmableOptions, SupplierId preferredSupplier) {
        // Every comparator below is "ascending = better" so a single min()
        // expresses all four steps as one strict tie-break chain.
        Comparator<SupplierSearchResult> byPreferredSupplierFirst = Comparator.comparingInt(
            (SupplierSearchResult r) -> (preferredSupplier != null && r.supplierId() == preferredSupplier) ? 0 : 1);
        Comparator<SupplierSearchResult> byBestMargin =
            Comparator.comparing((SupplierSearchResult r) -> r.netRate().amount()); // lower net rate first (placeholder margin proxy)
        Comparator<SupplierSearchResult> byRating = Comparator.comparing(
            (SupplierSearchResult r) -> r.rating() != null ? -r.rating() : Double.POSITIVE_INFINITY);

        return confirmableOptions.stream()
            .min(byPreferredSupplierFirst.thenComparing(byBestMargin).thenComparing(byRating));
    }
}
