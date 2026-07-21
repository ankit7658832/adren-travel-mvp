package com.adren.travel.infra;

import com.adren.travel.whitelabel.Market;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TST-07 — representative Consultant/Itinerary/Package fixtures across all
 * six markets (PRD §13.1, §17), so module/integrationTests exercising
 * market-dependent logic (KYC, GST/TCS, ATOL, currency) don't each invent
 * their own. Pure JDBC inserts, not calls through {@code WhitelabelApi}/
 * {@code BookingApi} or a real HTTP server — the same established
 * convention every existing integration test already uses for Itinerary
 * (there is no REST endpoint that creates one; see
 * {@code FullVerticalSliceEndToEndIT.insertDraftItinerary}'s own comment),
 * extended here to Consultant/Package so this works identically from a
 * plain {@code test}-tier test with any {@code JdbcTemplate} and from a
 * TestInfrastructure-backed {@code integrationTest}, with no dependency on
 * a running Spring context, security principal, or HTTP server.
 */
public final class MarketSeedFixtures {

    /** Consultant/Package entities are package-private (RULES.md S4.2) — these plain records carry just the IDs/flags a caller needs. */
    public record ConsultantFixture(UUID consultantId, Market market) {
    }

    public record PackageFixture(UUID consultantId, UUID itineraryId, UUID packageId, boolean dynamicFlightHotelCombo) {
    }

    // Mirrors MarketKycRuleProvider's required-field set (whitelabel.internal,
    // not accessible here) — required fields only, seed-realistic values.
    private static final Map<Market, Map<String, String>> REQUIRED_KYC_FIELDS = Map.of(
        Market.INDIA, Map.of("gstRegistration", "GST123", "businessPan", "PAN123", "bankDetails", "IFSC0001/12345"),
        Market.AUSTRALIA, Map.of("abn", "ABN123456789", "bankDetails", "BSB062-000/12345678"),
        Market.UK, Map.of("companiesHouseNumber", "CH12345678", "bankDetails", "SORT12-34-56/12345678"),
        Market.USA, Map.of("einBusinessRegistration", "EIN12-3456789", "bankDetails", "ROUTING123456789/12345678"),
        Market.DUBAI_UAE, Map.of("dtcmTradeLicense", "DTCM123456", "bankDetails", "IBANAE1234567890"),
        Market.DENMARK, Map.of("cvrRegistrationNumber", "CVR12345678", "bankDetails", "REG1234/1234567890")
    );

    private MarketSeedFixtures() {
    }

    /** One Consultant, seeded with that market's real required KYC field set (per MarketKycRuleProvider). */
    public static ConsultantFixture seedConsultant(JdbcTemplate jdbcTemplate, Market market) {
        UUID consultantId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO consultant (consultant_id, business_name, home_market, status, created_at) " +
                "VALUES (?, ?, ?, 'ACTIVE', now())",
            consultantId, "Seed Consultant (" + market + ")", market.name());

        REQUIRED_KYC_FIELDS.getOrDefault(market, Map.of()).forEach((key, value) ->
            jdbcTemplate.update(
                "INSERT INTO consultant_kyc_field (consultant_id, field_key, field_value) VALUES (?, ?, ?)",
                consultantId, key, value));

        return new ConsultantFixture(consultantId, market);
    }

    /** One Consultant per market — all six, in enum order. */
    public static List<ConsultantFixture> seedConsultantPerMarket(JdbcTemplate jdbcTemplate) {
        return List.of(Market.values()).stream()
            .map(market -> seedConsultant(jdbcTemplate, market))
            .toList();
    }

    /**
     * This story's own acceptance criterion: a UK Consultant with a
     * dynamic flight+hotel Package (PRD §17.2's ATOL trigger, BOK-11's
     * gate) — {@code dynamicFlightHotelCombo=true}, ATOL disclosure NOT
     * yet completed, so a test can exercise either the "blocked" or the
     * "completes disclosure then publishes" half of BOK-11's gate.
     */
    public static PackageFixture seedUkDynamicFlightHotelComboPackage(JdbcTemplate jdbcTemplate) {
        ConsultantFixture consultant = seedConsultant(jdbcTemplate, Market.UK);

        UUID itineraryId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO itinerary (itinerary_id, consultant_id, status, ai_generated, created_at, updated_at) " +
                "VALUES (?, ?, 'QUOTATION', false, now(), now())",
            itineraryId, consultant.consultantId());

        UUID packageId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO travel_package (package_id, source_itinerary_id, consultant_id, name, description, " +
                "validity_start, validity_end, base_price, markup_price, currency, max_pax, " +
                "dynamic_flight_hotel_combo, status, created_at) " +
                "VALUES (?, ?, ?, 'Seed UK Dynamic Combo Package', 'Flight + hotel seed fixture (BOK-11, PRD S17.2)', " +
                "CURRENT_DATE, CURRENT_DATE + INTERVAL '90 days', 1000, 1200, 'GBP', 2, true, 'DRAFT', now())",
            packageId, itineraryId, consultant.consultantId());

        return new PackageFixture(consultant.consultantId(), itineraryId, packageId, true);
    }
}
