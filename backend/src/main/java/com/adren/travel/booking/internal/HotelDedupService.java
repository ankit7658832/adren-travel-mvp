package com.adren.travel.booking.internal;

import com.adren.travel.supplier.SupplierSearchResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Deduplicates the same physical hotel property offered by two-or-more
 * suppliers — e.g. Hotelbeds and STUBA both returning "Taj Palace" —
 * before {@link DefaultSelectionService} runs (PRD §9.4, §22.2, BOK-20).
 * <p>
 * Matching is name-based only: {@link SupplierSearchResult} (the shared
 * normalized shape every {@code HotelbedsClient}/{@code StubaClient}/{@code
 * TboClient} result already returns) carries no geo/address fields, so a
 * "name/geo/address heuristic" narrows in practice to what's actually on
 * the type — property name, normalized (lowercased, punctuation/whitespace
 * stripped) to absorb minor formatting differences across suppliers (e.g.
 * "Taj Palace" vs. "Taj  Palace,"). Extending this to real geo/address
 * matching is future work once a supplier client's normalized result
 * carries that data (none does today).
 */
@Component
class HotelDedupService {

    List<SupplierSearchResult> deduplicate(List<SupplierSearchResult> results) {
        Map<String, SupplierSearchResult> bestByNormalizedName = new LinkedHashMap<>();
        for (SupplierSearchResult result : results) {
            bestByNormalizedName.merge(normalize(result.propertyName()), result, this::preferred);
        }
        return List.copyOf(bestByNormalizedName.values());
    }

    /**
     * Prefers the lower net rate as the existing margin-proxy convention
     * ({@link DefaultSelectionService}) already establishes, then rating.
     * Only compares net rates when both are in the same currency — this
     * runs at search time, before any FX conversion exists (FIN-04's
     * snapshot is a quotation-time concept) — a cross-currency pair keeps
     * whichever was seen first rather than comparing raw, currency-mismatched
     * numbers.
     */
    private SupplierSearchResult preferred(SupplierSearchResult existing, SupplierSearchResult candidate) {
        if (existing.netRate().currency() == candidate.netRate().currency()) {
            int byNetRate = existing.netRate().amount().compareTo(candidate.netRate().amount());
            if (byNetRate != 0) {
                return byNetRate < 0 ? existing : candidate;
            }
        }
        double existingRating = existing.rating() != null ? existing.rating() : Double.NEGATIVE_INFINITY;
        double candidateRating = candidate.rating() != null ? candidate.rating() : Double.NEGATIVE_INFINITY;
        return existingRating >= candidateRating ? existing : candidate;
    }

    private static String normalize(String propertyName) {
        if (propertyName == null) {
            return "";
        }
        return propertyName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
