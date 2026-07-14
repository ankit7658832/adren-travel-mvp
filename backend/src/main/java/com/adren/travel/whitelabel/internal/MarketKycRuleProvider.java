package com.adren.travel.whitelabel.internal;

import com.adren.travel.whitelabel.KycFieldDefinition;
import com.adren.travel.whitelabel.Market;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * PRD §13.1's per-market KYC table, as data — RULES.md §24.7 requires this
 * be a lookup, never a hardcoded per-market {@code if}/{@code switch}
 * scattered through onboarding logic. Adding a market or changing a
 * market's required fields is a one-line change here, not a code path
 * change at every call site.
 */
@Component
class MarketKycRuleProvider {

    private static final Map<Market, List<KycFieldDefinition>> RULES = Map.of(
        Market.INDIA, List.of(
            new KycFieldDefinition("gstRegistration", "GST Registration", true),
            new KycFieldDefinition("businessPan", "Business PAN", true),
            new KycFieldDefinition("iataTaaiNumber", "IATA/TAAI Number", false),
            new KycFieldDefinition("bankDetails", "Bank Details", true)
        ),
        Market.AUSTRALIA, List.of(
            new KycFieldDefinition("abn", "ABN", true),
            new KycFieldDefinition("atasAccreditation", "ATAS Accreditation", false),
            new KycFieldDefinition("bankDetails", "Bank Details", true)
        ),
        Market.UK, List.of(
            new KycFieldDefinition("companiesHouseNumber", "Companies House Number", true),
            new KycFieldDefinition("atolLicense", "ATOL License", false),
            new KycFieldDefinition("bankDetails", "Bank Details", true)
        ),
        Market.USA, List.of(
            new KycFieldDefinition("einBusinessRegistration", "EIN/Business Registration", true),
            new KycFieldDefinition("stateSellerOfTravelRegistration", "State Seller of Travel Registration", false),
            new KycFieldDefinition("bankDetails", "Bank Details", true)
        ),
        Market.DUBAI_UAE, List.of(
            new KycFieldDefinition("dtcmTradeLicense", "DTCM Trade License", true),
            new KycFieldDefinition("bankDetails", "Bank Details", true)
        ),
        Market.DENMARK, List.of(
            new KycFieldDefinition("cvrRegistrationNumber", "CVR Registration Number", true),
            new KycFieldDefinition("bankDetails", "Bank Details", true)
        )
    );

    List<KycFieldDefinition> requiredFieldsFor(Market market) {
        return RULES.getOrDefault(market, List.of());
    }
}
