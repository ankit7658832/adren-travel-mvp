package com.adren.travel.supplier;

/**
 * Canonical supplier identifier (PRD §10.1) — used both by normalized
 * search results and by FND-10's Adren-owned credential management.
 * {@code LOCAL_DMC}/{@code BYOS} aren't Adren-owned API integrations (manual
 * onboarding and per-Consultant credentials respectively), so they never
 * get a {@code SupplierCredential} row.
 */
public enum SupplierId {
    HOTELBEDS, STUBA, TBO, MYSTIFLY, TRANSFERZ, WIDGETY, HBACTIVITIES, LOCAL_DMC, BYOS
}
