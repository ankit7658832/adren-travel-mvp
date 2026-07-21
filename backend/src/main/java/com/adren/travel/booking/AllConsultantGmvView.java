package com.adren.travel.booking;

import com.adren.travel.shared.CurrencyAmount;

import java.util.List;

/** HRD-11, PRD §9.5/§21.6 — the Super Admin Dashboard's all-Consultant GMV, platform scope, grouped by settlement currency. */
public record AllConsultantGmvView(List<CurrencyAmount> gmvByCurrency) {
}
