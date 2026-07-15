package com.adren.travel.notification.internal;

import com.adren.travel.whitelabel.Market;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * PRD §15/§22.7 T11's per-market secondary-channel default, as data —
 * RULES.md §24.7 requires this be a lookup, never a hardcoded per-market
 * {@code if}/{@code switch}, mirroring {@code whitelabel.internal.MarketKycRuleProvider}'s
 * shape. Per-Consultant override (HRD-04's notification preferences
 * screen) is not yet built, so this default always applies.
 */
@Component
class SecondaryChannelProvider {

    private static final Map<Market, NotificationChannel> DEFAULT_SECONDARY_CHANNEL = Map.of(
        Market.INDIA, NotificationChannel.WHATSAPP,
        Market.DUBAI_UAE, NotificationChannel.WHATSAPP,
        Market.UK, NotificationChannel.SMS,
        Market.USA, NotificationChannel.SMS,
        Market.AUSTRALIA, NotificationChannel.SMS,
        Market.DENMARK, NotificationChannel.SMS
    );

    NotificationChannel defaultChannelFor(Market market) {
        return DEFAULT_SECONDARY_CHANNEL.getOrDefault(market, NotificationChannel.SMS);
    }
}
