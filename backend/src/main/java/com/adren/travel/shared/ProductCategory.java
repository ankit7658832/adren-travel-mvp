package com.adren.travel.shared;

/**
 * The five line-item product categories PRD §9.1/§12.1 name (Hotel/Flight/
 * Transfer/Cruise/Activity) — a plain enum in {@code shared} (mirrors
 * {@link CurrencyCode}'s shape) since both {@code booking} (line items)
 * and {@code payments} (per-category markup rules, FIN-01) need it.
 */
public enum ProductCategory {
    HOTEL, FLIGHT, TRANSFER, CRUISE, ACTIVITY
}
