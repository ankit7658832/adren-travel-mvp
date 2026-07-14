package com.adren.travel.whitelabel.internal;

import com.adren.travel.whitelabel.KycFieldDefinition;
import com.adren.travel.whitelabel.Market;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarketKycRuleProviderTest {

    private final MarketKycRuleProvider provider = new MarketKycRuleProvider();

    @Test
    void indiaRequiresGstRegistrationAndBusinessPan() {
        var fields = provider.requiredFieldsFor(Market.INDIA);

        assertThat(fields).extracting(KycFieldDefinition::fieldKey)
            .contains("gstRegistration", "businessPan", "bankDetails");
        assertThat(fields).filteredOn(f -> f.fieldKey().equals("gstRegistration"))
            .allMatch(KycFieldDefinition::required);
    }

    @Test
    void usaRequiresEinButStateSellerOfTravelRegistrationIsOptional() {
        var fields = provider.requiredFieldsFor(Market.USA);

        assertThat(fields).filteredOn(f -> f.fieldKey().equals("einBusinessRegistration"))
            .allMatch(KycFieldDefinition::required);
        assertThat(fields).filteredOn(f -> f.fieldKey().equals("stateSellerOfTravelRegistration"))
            .noneMatch(KycFieldDefinition::required);
    }

    @Test
    void everyMarketHasAtLeastOneRequiredField() {
        for (Market market : Market.values()) {
            assertThat(provider.requiredFieldsFor(market)).anyMatch(KycFieldDefinition::required);
        }
    }
}
