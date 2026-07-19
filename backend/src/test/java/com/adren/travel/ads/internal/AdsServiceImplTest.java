package com.adren.travel.ads.internal;

import com.adren.travel.ads.AdAccountView;
import com.adren.travel.ads.AdCampaignView;
import com.adren.travel.ads.CreateCampaignCommand;
import com.adren.travel.ai.AiApi;
import com.adren.travel.booking.BookingApi;
import com.adren.travel.booking.PackageView;
import com.adren.travel.shared.CurrencyCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdsServiceImplTest {

    @Mock
    BookingApi bookingApi;

    @Mock
    AiApi aiApi;

    @Mock
    AdAccountRepository adAccountRepository;

    @Mock
    MetaAdsClient metaAdsClient;

    @Mock
    AdCampaignRepository adCampaignRepository;

    @Mock
    ApplicationEventPublisher events;

    private AdsServiceImpl service() {
        return new AdsServiceImpl(bookingApi, aiApi, adAccountRepository, metaAdsClient, adCampaignRepository, events);
    }

    @Test
    void provisionAdAccountCreatesANewAccountViaTheMetaClientWhenNoneExistsADS01() {
        UUID consultantId = UUID.randomUUID();
        when(adAccountRepository.findByConsultantId(consultantId)).thenReturn(Optional.empty());
        when(metaAdsClient.provisionAdAccount(consultantId)).thenReturn("stub-bm-123");
        when(adAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AdAccountView view = service().provisionAdAccount(consultantId);

        assertThat(view.consultantId()).isEqualTo(consultantId);
        assertThat(view.metaBusinessManagerId()).isEqualTo("stub-bm-123");
        verify(metaAdsClient).provisionAdAccount(consultantId);
        verify(adAccountRepository).save(any());
    }

    @Test
    void provisionAdAccountIsIdempotentReturningTheExistingAccountRatherThanProvisioningATwinADS01() {
        UUID consultantId = UUID.randomUUID();
        AdAccount existing = new AdAccount(UUID.randomUUID(), consultantId, "stub-bm-already-provisioned");
        when(adAccountRepository.findByConsultantId(consultantId)).thenReturn(Optional.of(existing));

        AdAccountView view = service().provisionAdAccount(consultantId);

        assertThat(view.metaBusinessManagerId()).isEqualTo("stub-bm-already-provisioned");
        verify(metaAdsClient, never()).provisionAdAccount(any());
        verify(adAccountRepository, never()).save(any());
    }

    @Test
    void createCampaignStartsInPendingApprovalWithThePackagesOwnCurrencyAndPublishesAnEventADS02() {
        UUID packageId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        PackageView travelPackage = new PackageView(packageId, UUID.randomUUID(), consultantId, "Goa Escape",
            "A relaxing package", LocalDate.now().plusDays(30), LocalDate.now().plusDays(90),
            BigDecimal.valueOf(20000), BigDecimal.valueOf(5000), CurrencyCode.INR, 4, false);
        when(bookingApi.findPackageById(packageId)).thenReturn(travelPackage);
        when(adCampaignRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AdCampaignView view = service().createCampaign(new CreateCampaignCommand(packageId));

        assertThat(view.status()).isEqualTo("PENDING_APPROVAL");
        assertThat(view.packageId()).isEqualTo(packageId);
        assertThat(view.consultantId()).isEqualTo(consultantId);
        assertThat(view.budgetCapCurrency()).isEqualTo(CurrencyCode.INR);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(com.adren.travel.ads.event.AdCampaignCreatedEvent.class);
    }
}
