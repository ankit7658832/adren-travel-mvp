package com.adren.travel.dashboard.internal;

import com.adren.travel.ads.AdCampaignView;
import com.adren.travel.ads.AdSpendAcrossConsultantsView;
import com.adren.travel.ads.AdsApi;
import com.adren.travel.ai.AiApi;
import com.adren.travel.ai.AiGovernanceSummaryView;
import com.adren.travel.booking.AllConsultantGmvView;
import com.adren.travel.booking.BookingApi;
import com.adren.travel.booking.ConsultantBookingMetricsView;
import com.adren.travel.booking.PackageSummaryView;
import com.adren.travel.booking.QuotationSummaryView;
import com.adren.travel.booking.SupplierPerformanceView;
import com.adren.travel.dashboard.ConsultantDashboardView;
import com.adren.travel.dashboard.SuperAdminDashboardView;
import com.adren.travel.payments.PaymentsApi;
import com.adren.travel.payments.WalletView;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** HRD-09/HRD-11 — DashboardServiceImpl is a pure orchestrator: every field it returns comes straight from another module's own Api, so these tests prove the wiring, not any calculation logic (already tested in each owning module). */
@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    BookingApi bookingApi;

    @Mock
    PaymentsApi paymentsApi;

    @Mock
    AdsApi adsApi;

    @Mock
    AiApi aiApi;

    private DashboardServiceImpl service() {
        return new DashboardServiceImpl(bookingApi, paymentsApi, adsApi, aiApi);
    }

    @Test
    void findConsultantDashboardComposesEveryModulesOwnViewHRD09() {
        UUID consultantId = UUID.randomUUID();
        ConsultantBookingMetricsView metrics = new ConsultantBookingMetricsView(3, new Money(BigDecimal.valueOf(15_000), CurrencyCode.INR));
        WalletView wallet = new WalletView(consultantId, BigDecimal.valueOf(50_000), BigDecimal.valueOf(100_000),
            BigDecimal.ZERO, CurrencyCode.INR, Instant.now());
        List<PackageSummaryView> topPackages = List.of(new PackageSummaryView(UUID.randomUUID(), "Goa Escape", 2));
        QuotationSummaryView quotation = new QuotationSummaryView(UUID.randomUUID(), Instant.now());
        AdCampaignView campaign = new AdCampaignView(UUID.randomUUID(), UUID.randomUUID(), consultantId, "LIVE",
            "Adults 25-45", BigDecimal.valueOf(500), CurrencyCode.INR, 14, "meta-ref", BigDecimal.ZERO, null,
            0, 0, 0, false, false, null);
        when(bookingApi.findConsultantBookingMetrics(consultantId)).thenReturn(metrics);
        when(paymentsApi.getWallet(consultantId)).thenReturn(wallet);
        when(bookingApi.findTopPackagesForConsultant(consultantId, 5)).thenReturn(topPackages);
        when(bookingApi.findPendingQuotationsForConsultant(eq(consultantId), any()))
            .thenReturn(new PageImpl<>(List.of(quotation)));
        when(adsApi.findCampaignsForConsultant(eq(consultantId), any()))
            .thenReturn(new PageImpl<>(List.of(campaign)));

        ConsultantDashboardView view = service().findConsultantDashboard(consultantId);

        assertThat(view.metrics()).isEqualTo(metrics);
        assertThat(view.wallet()).isEqualTo(wallet);
        assertThat(view.topPackages()).isEqualTo(topPackages);
        assertThat(view.pendingQuotations()).containsExactly(quotation);
        assertThat(view.activeCampaigns()).containsExactly(campaign);
    }

    @Test
    void findSuperAdminDashboardComposesEveryModulesOwnViewHRD11() {
        AllConsultantGmvView gmv = new AllConsultantGmvView(List.of());
        List<SupplierPerformanceView> supplierPerformance = List.of();
        AiGovernanceSummaryView aiSummary = new AiGovernanceSummaryView(10, 8, 1, 1);
        AdSpendAcrossConsultantsView adSpend = new AdSpendAcrossConsultantsView(List.of());
        when(bookingApi.findAllConsultantGmv()).thenReturn(gmv);
        when(bookingApi.findSupplierPerformanceSummary()).thenReturn(supplierPerformance);
        when(aiApi.findAiGovernanceSummary()).thenReturn(aiSummary);
        when(adsApi.findAdSpendAcrossConsultants()).thenReturn(adSpend);

        SuperAdminDashboardView view = service().findSuperAdminDashboard();

        assertThat(view.gmv()).isEqualTo(gmv);
        assertThat(view.supplierPerformance()).isEqualTo(supplierPerformance);
        assertThat(view.aiGovernanceSummary()).isEqualTo(aiSummary);
        assertThat(view.adSpend()).isEqualTo(adSpend);
    }
}
