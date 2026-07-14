package com.adren.travel.whitelabel.internal;

import com.adren.travel.whitelabel.Market;
import com.adren.travel.whitelabel.OnboardConsultantCommand;
import com.adren.travel.whitelabel.event.ConsultantOnboardedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WhitelabelServiceImplTest {

    @Mock
    ConsultantRepository consultantRepository;

    @Mock
    ApplicationEventPublisher events;

    WhitelabelServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WhitelabelServiceImpl(consultantRepository, new MarketKycRuleProvider(), events);
    }

    @Test
    void onboardsAConsultantWithAllRequiredKycFieldsAndPublishesEvent() {
        var command = new OnboardConsultantCommand("Test Travel Co", Market.INDIA, Map.of(
            "gstRegistration", "GST123",
            "businessPan", "PAN123",
            "bankDetails", "Bank ABC"
        ));

        var consultantId = service.onboardConsultant(command);

        assertThat(consultantId).isNotNull();
        verify(consultantRepository).save(org.mockito.ArgumentMatchers.any());

        ArgumentCaptor<ConsultantOnboardedEvent> captor = ArgumentCaptor.forClass(ConsultantOnboardedEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().consultantId()).isEqualTo(consultantId);
        assertThat(captor.getValue().homeMarket()).isEqualTo(Market.INDIA);
    }

    @Test
    void rejectsOnboardingWhenARequiredKycFieldIsMissing() {
        var command = new OnboardConsultantCommand("Test Travel Co", Market.INDIA, Map.of(
            "gstRegistration", "GST123"
            // missing businessPan, bankDetails
        ));

        assertThatThrownBy(() -> service.onboardConsultant(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("businessPan");
    }

    @Test
    void rejectsOnboardingWhenKycFieldsIsNullEntirely() {
        var command = new OnboardConsultantCommand("Test Travel Co", Market.DENMARK, null);

        assertThatThrownBy(() -> service.onboardConsultant(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cvrRegistrationNumber");
    }

    @Test
    void requiredKycFieldsForDelegatesToTheRuleProvider() {
        assertThat(service.requiredKycFieldsFor(Market.UK)).extracting("fieldKey")
            .contains("companiesHouseNumber");
    }
}
