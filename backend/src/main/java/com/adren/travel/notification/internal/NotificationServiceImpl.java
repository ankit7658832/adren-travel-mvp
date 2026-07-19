package com.adren.travel.notification.internal;

import com.adren.travel.notification.NotificationApi;
import com.adren.travel.notification.NotificationPreferenceView;
import com.adren.travel.notification.UpdateNotificationPreferenceCommand;
import com.adren.travel.notification.event.NotificationPreferenceUpdatedEvent;
import com.adren.travel.security.CurrentPrincipal;
import com.adren.travel.whitelabel.Market;
import com.adren.travel.whitelabel.WhitelabelApi;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * HRD-04 — a Consultant's own secondary-channel preference override.
 * {@code updateNotificationPreference}/{@code findNotificationPreference}
 * always resolve the consultantId from {@link CurrentPrincipal}, never a
 * caller-supplied value (RULES.md §5.2) — there is no consultantId
 * parameter on either method to spoof in the first place, the same
 * structural guarantee {@code ByosCredentialService#readForCurrentConsultant}
 * established.
 */
@Service
class NotificationServiceImpl implements NotificationApi {

    private final NotificationPreferenceRepository repository;
    private final WhitelabelApi whitelabelApi;
    private final SecondaryChannelProvider secondaryChannelProvider;
    private final ApplicationEventPublisher events;

    NotificationServiceImpl(NotificationPreferenceRepository repository, WhitelabelApi whitelabelApi,
                            SecondaryChannelProvider secondaryChannelProvider, ApplicationEventPublisher events) {
        this.repository = repository;
        this.whitelabelApi = whitelabelApi;
        this.secondaryChannelProvider = secondaryChannelProvider;
        this.events = events;
    }

    @Override
    @Transactional
    public void updateNotificationPreference(UpdateNotificationPreferenceCommand command) {
        UUID consultantId = CurrentPrincipal.get().consultantId();
        NotificationChannel channel = parseChannel(command.secondaryChannel());

        NotificationPreference preference = repository.findById(consultantId)
            .map(existing -> {
                existing.override(channel);
                return existing;
            })
            .orElseGet(() -> new NotificationPreference(consultantId, channel));
        repository.save(preference);

        events.publishEvent(new NotificationPreferenceUpdatedEvent(consultantId, channel.name()));
    }

    @Override
    public NotificationPreferenceView findNotificationPreference() {
        UUID consultantId = CurrentPrincipal.get().consultantId();
        return repository.findById(consultantId)
            .map(preference -> new NotificationPreferenceView(preference.getSecondaryChannel().name(), true))
            .orElseGet(() -> new NotificationPreferenceView(marketDefaultChannel(consultantId).name(), false));
    }

    private NotificationChannel marketDefaultChannel(UUID consultantId) {
        try {
            Market market = whitelabelApi.findConsultantMarket(consultantId);
            return secondaryChannelProvider.defaultChannelFor(market);
        } catch (IllegalArgumentException e) {
            return NotificationChannel.SMS;
        }
    }

    private static NotificationChannel parseChannel(String secondaryChannel) {
        try {
            return NotificationChannel.valueOf(secondaryChannel);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Unknown secondary channel: " + secondaryChannel);
        }
    }
}
