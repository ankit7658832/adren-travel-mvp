package com.adren.travel.ai;

/**
 * One generated ad-creative variant (PRD §14.4, AI-12) — {@code bodyText}
 * is guaranteed (server-side, not just prompted) to literally reference the
 * Package's real name and exact current sell price; see {@code
 * AiServiceImpl}'s Javadoc for the grounding check that enforces this.
 */
public record AdCreativeVariant(String headline, String bodyText) {
}
