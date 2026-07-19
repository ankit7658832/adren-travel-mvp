package com.adren.travel.notification.event;

import java.util.UUID;

/** Published whenever a Consultant saves a secondary-channel override (PRD §21.10, HRD-04). No consumer yet. */
public record NotificationPreferenceUpdatedEvent(UUID consultantId, String secondaryChannel) {
}
