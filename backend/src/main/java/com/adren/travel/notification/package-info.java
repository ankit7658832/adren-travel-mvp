/**
 * Notification module (PRD Section 15). Reacts to events published by other
 * modules (primarily {@code BookingConfirmedEvent} from the Booking module)
 * — it has no public API of its own in MVP scope, it is purely a consumer.
 * Also depends directly on {@code whitelabel.WhitelabelApi} (HRD-01) to
 * resolve a Consultant's home market for region-routed secondary-channel
 * dispatch (WhatsApp for India/Dubai, SMS elsewhere).
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Notifications"
)
package com.adren.travel.notification;
