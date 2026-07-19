package com.adren.travel.ads.internal;

import com.adren.travel.ads.AdAccountView;
import com.adren.travel.ai.AiApi;
import com.adren.travel.booking.BookingApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private AdsServiceImpl service() {
        return new AdsServiceImpl(bookingApi, aiApi, adAccountRepository, metaAdsClient);
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
}
