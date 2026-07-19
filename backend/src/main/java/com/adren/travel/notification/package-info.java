/**
 * Notification module (PRD Section 15). Reacts to events published by other
 * modules (primarily {@code BookingConfirmedEvent} from the Booking module)
 * — mostly a consumer. {@link com.adren.travel.notification.NotificationApi}
 * (HRD-04) is its first real public surface: a Consultant's own secondary-
 * channel preference override. Also depends directly on {@code
 * whitelabel.WhitelabelApi} (HRD-01) to resolve a Consultant's home market
 * for region-routed secondary-channel dispatch (WhatsApp for India/Dubai,
 * SMS elsewhere) when no override is saved.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Notifications"
)
package com.adren.travel.notification;
