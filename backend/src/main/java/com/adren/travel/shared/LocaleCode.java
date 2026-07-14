package com.adren.travel.shared;

/**
 * Display-language codes PRD §13.3's multi-language requirement covers —
 * mirrors {@link CurrencyCode}'s shape (a plain enum in {@code shared}, so
 * any module, e.g. {@code notification}'s per-region channel language, can
 * depend on it without reaching into {@code whitelabel.internal}). English
 * is always available and is every market's default/primary language;
 * {@code HI}/{@code DA} are the secondary options PRD §13.3 names for
 * India/Denmark respectively — see {@code MarketLocaleProvider} for the
 * per-market data-driven mapping (RULES.md §24.7).
 */
public enum LocaleCode {
    EN, HI, DA
}
