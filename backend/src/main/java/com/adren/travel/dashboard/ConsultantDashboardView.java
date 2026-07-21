package com.adren.travel.dashboard;

import com.adren.travel.ads.AdCampaignView;
import com.adren.travel.booking.ConsultantBookingMetricsView;
import com.adren.travel.booking.PackageSummaryView;
import com.adren.travel.booking.QuotationSummaryView;
import com.adren.travel.payments.WalletView;

import java.util.List;

/** PRD §9.5/§21.5, HRD-09 — the Consultant Dashboard's composite view, never a JPA entity, sourced entirely from other modules' own public views. */
public record ConsultantDashboardView(
    ConsultantBookingMetricsView metrics,
    WalletView wallet,
    List<PackageSummaryView> topPackages,
    List<QuotationSummaryView> pendingQuotations,
    List<AdCampaignView> activeCampaigns
) {
}
