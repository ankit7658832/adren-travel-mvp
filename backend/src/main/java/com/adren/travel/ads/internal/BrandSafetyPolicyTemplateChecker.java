package com.adren.travel.ads.internal;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * The rule-based (not AI-driven) first-pass brand-safety check (PRD
 * §14.3, ADS-15) layered ahead of ADS-06's manual policy-review queue —
 * flags obvious violations for the Super Admin without auto-rejecting
 * anything (this story's own AC). Case-insensitive substring match
 * against each creative variant's headline/body text, driven entirely by
 * {@link BrandSafetyPolicyTemplateProperties}' configured phrase list.
 */
@Component
class BrandSafetyPolicyTemplateChecker {

    private final BrandSafetyPolicyTemplateProperties properties;

    BrandSafetyPolicyTemplateChecker(BrandSafetyPolicyTemplateProperties properties) {
        this.properties = properties;
    }

    /** Returns every configured banned phrase found across the given variants' headline/body text, empty if none. */
    List<String> checkViolations(List<AdCampaignCreativeVariant> variants) {
        List<String> bannedPhrases = properties.bannedPhrases();
        if (bannedPhrases == null || bannedPhrases.isEmpty()) {
            return List.of();
        }

        return bannedPhrases.stream()
            .filter(phrase -> variants.stream().anyMatch(variant -> containsPhrase(variant, phrase)))
            .toList();
    }

    private static boolean containsPhrase(AdCampaignCreativeVariant variant, String phrase) {
        String needle = phrase.toLowerCase(Locale.ROOT);
        String headline = variant.getHeadline() == null ? "" : variant.getHeadline().toLowerCase(Locale.ROOT);
        String bodyText = variant.getBodyText() == null ? "" : variant.getBodyText().toLowerCase(Locale.ROOT);
        return headline.contains(needle) || bodyText.contains(needle);
    }
}
