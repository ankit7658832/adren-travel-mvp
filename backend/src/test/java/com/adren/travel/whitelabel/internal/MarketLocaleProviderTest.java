package com.adren.travel.whitelabel.internal;

import com.adren.travel.shared.LocaleCode;
import com.adren.travel.whitelabel.Market;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** FND-17's core acceptance criterion: PRD §13.3's per-market language catalog, as data. */
class MarketLocaleProviderTest {

    private final MarketLocaleProvider provider = new MarketLocaleProvider();

    @Test
    void denmarkOffersDanishAlongsideEnglishPrimary() {
        assertThat(provider.availableLocalesFor(Market.DENMARK)).containsExactly(LocaleCode.EN, LocaleCode.DA);
    }

    @Test
    void indiaOffersHindiAlongsideEnglish() {
        assertThat(provider.availableLocalesFor(Market.INDIA)).containsExactly(LocaleCode.EN, LocaleCode.HI);
    }

    @Test
    void everyOtherMarketIsEnglishOnly() {
        assertThat(provider.availableLocalesFor(Market.AUSTRALIA)).containsExactly(LocaleCode.EN);
        assertThat(provider.availableLocalesFor(Market.UK)).containsExactly(LocaleCode.EN);
        assertThat(provider.availableLocalesFor(Market.USA)).containsExactly(LocaleCode.EN);
        assertThat(provider.availableLocalesFor(Market.DUBAI_UAE)).containsExactly(LocaleCode.EN);
    }
}
