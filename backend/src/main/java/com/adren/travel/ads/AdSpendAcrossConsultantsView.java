package com.adren.travel.ads;

import com.adren.travel.shared.CurrencyAmount;

import java.util.List;

/** HRD-11, PRD §9.5/§21.6 — the Super Admin Dashboard's ad spend across Consultants, platform scope, grouped by budget-cap currency. */
public record AdSpendAcrossConsultantsView(List<CurrencyAmount> spendByCurrency) {
}
