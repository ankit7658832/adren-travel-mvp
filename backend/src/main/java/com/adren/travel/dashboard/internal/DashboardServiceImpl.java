package com.adren.travel.dashboard.internal;

import com.adren.travel.ads.AdCampaignView;
import com.adren.travel.ads.AdsApi;
import com.adren.travel.ai.AiApi;
import com.adren.travel.booking.BookingApi;
import com.adren.travel.booking.PackageSummaryView;
import com.adren.travel.booking.QuotationSummaryView;
import com.adren.travel.dashboard.ConsultantDashboardView;
import com.adren.travel.dashboard.DashboardApi;
import com.adren.travel.dashboard.SuperAdminDashboardView;
import com.adren.travel.payments.PaymentsApi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Internal implementation of {@link DashboardApi}. Not visible outside
 * this module. Purely a composite-read orchestrator — every field comes
 * from another module's own public Api, each of which already enforces
 * its own tenant scoping/authorization (RULES.md §5.2), so this class
 * never duplicates those checks.
 */
@Service
class DashboardServiceImpl implements DashboardApi {

    /** HRD-09's own sub-task: "composite read endpoint (paginated sub-collections)" — bounded default page size for the dashboard's inline summary, not a full listing. */
    private static final int DASHBOARD_SUB_COLLECTION_PAGE_SIZE = 10;
    private static final int TOP_PACKAGES_LIMIT = 5;

    private final BookingApi bookingApi;
    private final PaymentsApi paymentsApi;
    private final AdsApi adsApi;
    private final AiApi aiApi;

    DashboardServiceImpl(BookingApi bookingApi, PaymentsApi paymentsApi, AdsApi adsApi, AiApi aiApi) {
        this.bookingApi = bookingApi;
        this.paymentsApi = paymentsApi;
        this.adsApi = adsApi;
        this.aiApi = aiApi;
    }

    @Override
    public ConsultantDashboardView findConsultantDashboard(UUID consultantId) {
        List<PackageSummaryView> topPackages = bookingApi.findTopPackagesForConsultant(consultantId, TOP_PACKAGES_LIMIT);
        Page<QuotationSummaryView> pendingQuotations = bookingApi.findPendingQuotationsForConsultant(
            consultantId, PageRequest.of(0, DASHBOARD_SUB_COLLECTION_PAGE_SIZE));
        Page<AdCampaignView> activeCampaigns = adsApi.findCampaignsForConsultant(
            consultantId, PageRequest.of(0, DASHBOARD_SUB_COLLECTION_PAGE_SIZE));

        return new ConsultantDashboardView(
            bookingApi.findConsultantBookingMetrics(consultantId),
            paymentsApi.getWallet(consultantId),
            topPackages,
            pendingQuotations.getContent(),
            activeCampaigns.getContent());
    }

    @Override
    public SuperAdminDashboardView findSuperAdminDashboard() {
        return new SuperAdminDashboardView(
            bookingApi.findAllConsultantGmv(),
            bookingApi.findSupplierPerformanceSummary(),
            aiApi.findAiGovernanceSummary(),
            adsApi.findAdSpendAcrossConsultants());
    }
}
