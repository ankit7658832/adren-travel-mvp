package com.adren.travel.whitelabel.internal;

import com.adren.travel.shared.LocaleCode;
import com.adren.travel.whitelabel.Market;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * PRD §13.3's per-market available-language table, as data — mirrors
 * {@code MarketKycRuleProvider}'s shape (RULES.md §24.7: a lookup, never a
 * hardcoded per-market {@code if}/{@code switch}). English is always
 * offered and is every market's primary language; only India (Hindi) and
 * Denmark (Danish) currently have a secondary option per the story's ACs.
 */
@Component
class MarketLocaleProvider {

    private static final Map<Market, List<LocaleCode>> AVAILABLE_LOCALES = Map.of(
        Market.INDIA, List.of(LocaleCode.EN, LocaleCode.HI),
        Market.AUSTRALIA, List.of(LocaleCode.EN),
        Market.UK, List.of(LocaleCode.EN),
        Market.USA, List.of(LocaleCode.EN),
        Market.DUBAI_UAE, List.of(LocaleCode.EN),
        Market.DENMARK, List.of(LocaleCode.EN, LocaleCode.DA)
    );

    List<LocaleCode> availableLocalesFor(Market market) {
        return AVAILABLE_LOCALES.getOrDefault(market, List.of(LocaleCode.EN));
    }
}
