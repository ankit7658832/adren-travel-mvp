package com.adren.travel.shared;

/**
 * The six settlement currencies from PRD Section 12.2. INR is the compulsory
 * home-market currency (Adren is India-based); the rest are expansion-market
 * currencies. Do not add a currency here without updating the FX buffer and
 * KYC/compliance logic that key off this enum (PRD Sections 12.2, 13.1, 17.1).
 */
public enum CurrencyCode {
    INR, // Compulsory home-market currency (India)
    AUD,
    GBP,
    USD,
    AED,
    DKK
}
