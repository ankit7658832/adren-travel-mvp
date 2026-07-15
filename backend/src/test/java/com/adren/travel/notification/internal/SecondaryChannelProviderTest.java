package com.adren.travel.notification.internal;

import com.adren.travel.whitelabel.Market;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** PRD §15/§22.7 T11 — WhatsApp for India/Dubai, SMS everywhere else. */
class SecondaryChannelProviderTest {

    private final SecondaryChannelProvider provider = new SecondaryChannelProvider();

    @ParameterizedTest
    @CsvSource({
        "INDIA, WHATSAPP",
        "DUBAI_UAE, WHATSAPP",
        "UK, SMS",
        "USA, SMS",
        "AUSTRALIA, SMS",
        "DENMARK, SMS"
    })
    void mapsEachMarketToItsDocumentedDefaultChannel(Market market, NotificationChannel expectedChannel) {
        assertThat(provider.defaultChannelFor(market)).isEqualTo(expectedChannel);
    }
}
