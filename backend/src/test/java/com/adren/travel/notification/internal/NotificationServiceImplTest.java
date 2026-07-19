package com.adren.travel.notification.internal;

import com.adren.travel.notification.NotificationPreferenceView;
import com.adren.travel.notification.UpdateNotificationPreferenceCommand;
import com.adren.travel.notification.event.NotificationPreferenceUpdatedEvent;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.whitelabel.Market;
import com.adren.travel.whitelabel.WhitelabelApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HRD-04's core acceptance criteria: the regional default is pre-selected until a Consultant saves their own override. */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    NotificationPreferenceRepository repository;

    @Mock
    WhitelabelApi whitelabelApi;

    @Mock
    ApplicationEventPublisher events;

    NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(repository, whitelabelApi, new SecondaryChannelProvider(), events);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findNotificationPreferenceReturnsTheMarketDefaultWhenNoOverrideIsSavedFND03() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(consultantId);
        when(repository.findById(consultantId)).thenReturn(Optional.empty());
        when(whitelabelApi.findConsultantMarket(consultantId)).thenReturn(Market.INDIA);

        NotificationPreferenceView result = service.findNotificationPreference();

        assertThat(result.secondaryChannel()).isEqualTo("WHATSAPP");
        assertThat(result.isOverride()).isFalse();
    }

    @Test
    void findNotificationPreferenceReturnsTheSavedOverrideWhenOneExists() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(consultantId);
        when(repository.findById(consultantId))
            .thenReturn(Optional.of(new NotificationPreference(consultantId, NotificationChannel.SMS)));

        NotificationPreferenceView result = service.findNotificationPreference();

        assertThat(result.secondaryChannel()).isEqualTo("SMS");
        assertThat(result.isOverride()).isTrue();
    }

    @Test
    void updateNotificationPreferenceSavesANewOverrideAndPublishesTheEvent() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(consultantId);
        when(repository.findById(consultantId)).thenReturn(Optional.empty());

        service.updateNotificationPreference(new UpdateNotificationPreferenceCommand("WHATSAPP"));

        ArgumentCaptor<NotificationPreference> captor = ArgumentCaptor.forClass(NotificationPreference.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getConsultantId()).isEqualTo(consultantId);
        assertThat(captor.getValue().getSecondaryChannel()).isEqualTo(NotificationChannel.WHATSAPP);

        ArgumentCaptor<NotificationPreferenceUpdatedEvent> eventCaptor =
            ArgumentCaptor.forClass(NotificationPreferenceUpdatedEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().consultantId()).isEqualTo(consultantId);
        assertThat(eventCaptor.getValue().secondaryChannel()).isEqualTo("WHATSAPP");
    }

    @Test
    void updateNotificationPreferenceOverwritesAnExistingOverride() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(consultantId);
        NotificationPreference existing = new NotificationPreference(consultantId, NotificationChannel.SMS);
        when(repository.findById(consultantId)).thenReturn(Optional.of(existing));

        service.updateNotificationPreference(new UpdateNotificationPreferenceCommand("WHATSAPP"));

        verify(repository).save(existing);
        assertThat(existing.getSecondaryChannel()).isEqualTo(NotificationChannel.WHATSAPP);
    }

    @Test
    void updateNotificationPreferenceRejectsAnUnknownChannel() {
        authenticateAs(UUID.randomUUID());

        assertThatThrownBy(() -> service.updateNotificationPreference(new UpdateNotificationPreferenceCommand("CARRIER_PIGEON")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateNotificationPreferenceAlwaysScopesToTheCallingConsultantFND03() {
        // No consultantId parameter exists on the command at all — the
        // structural guarantee this test documents, mirroring
        // ByosCredentialService#readForCurrentConsultant.
        assertThat(UpdateNotificationPreferenceCommand.class.getDeclaredFields()).hasSize(1);
    }

    private static void authenticateAs(UUID consultantId) {
        AdrenPrincipal principal = new AdrenPrincipal(UUID.randomUUID(), Role.CONSULTANT, consultantId);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CONSULTANT"));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
