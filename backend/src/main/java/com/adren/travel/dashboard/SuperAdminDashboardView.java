package com.adren.travel.dashboard;

import com.adren.travel.ads.AdSpendAcrossConsultantsView;
import com.adren.travel.ai.AiGovernanceSummaryView;
import com.adren.travel.booking.AllConsultantGmvView;
import com.adren.travel.booking.SupplierPerformanceView;

import java.util.List;

/** PRD §9.5/§21.6, HRD-11 — the Super Admin Dashboard's composite view, never a JPA entity, sourced entirely from other modules' own public views. */
public record SuperAdminDashboardView(
    AllConsultantGmvView gmv,
    List<SupplierPerformanceView> supplierPerformance,
    AiGovernanceSummaryView aiGovernanceSummary,
    AdSpendAcrossConsultantsView adSpend
) {
}
