package com.adren.travel.ads.internal;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** ADS-15 — the rule-based (not AI-driven) brand-safety template check, driven entirely by the configured phrase list. */
class BrandSafetyPolicyTemplateCheckerTest {

    private AdCampaignCreativeVariant variant(String headline, String bodyText) {
        return new AdCampaignCreativeVariant(UUID.randomUUID(), UUID.randomUUID(), headline, bodyText, null);
    }

    @Test
    void checkViolationsReturnsEveryConfiguredPhraseFoundInAnyVariant() {
        var checker = new BrandSafetyPolicyTemplateChecker(
            new BrandSafetyPolicyTemplateProperties(List.of("guaranteed", "risk-free")));
        var variants = List.of(
            variant("Guaranteed lowest price!", "Book today"),
            variant("Escape to Goa", "A relaxing beach package"));

        List<String> violations = checker.checkViolations(variants);

        assertThat(violations).containsExactly("guaranteed");
    }

    @Test
    void checkViolationsIsCaseInsensitiveAndChecksBothHeadlineAndBodyText() {
        var checker = new BrandSafetyPolicyTemplateChecker(
            new BrandSafetyPolicyTemplateProperties(List.of("RISK-FREE")));
        var variants = List.of(variant("Escape to Goa", "A 100% risk-free getaway"));

        List<String> violations = checker.checkViolations(variants);

        assertThat(violations).containsExactly("RISK-FREE");
    }

    @Test
    void checkViolationsReturnsEmptyWhenNothingMatches() {
        var checker = new BrandSafetyPolicyTemplateChecker(
            new BrandSafetyPolicyTemplateProperties(List.of("guaranteed")));
        var variants = List.of(variant("Escape to Goa", "A relaxing beach package"));

        assertThat(checker.checkViolations(variants)).isEmpty();
    }

    @Test
    void checkViolationsReturnsEmptyWhenNoPhrasesAreConfigured() {
        var checker = new BrandSafetyPolicyTemplateChecker(new BrandSafetyPolicyTemplateProperties(List.of()));
        var variants = List.of(variant("Guaranteed lowest price!", "Book today"));

        assertThat(checker.checkViolations(variants)).isEmpty();
    }
}
