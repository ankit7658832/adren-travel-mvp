/**
 * Notification module (PRD Section 15). Reacts to events published by other
 * modules (primarily {@code BookingConfirmedEvent} from the Booking module)
 * — it has no public API of its own in MVP scope, it is purely a consumer.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Notifications"
)
package com.adren.travel.notification;
