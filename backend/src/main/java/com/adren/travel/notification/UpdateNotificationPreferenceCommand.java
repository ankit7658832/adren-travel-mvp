package com.adren.travel.notification;

/**
 * PRD §21.10, HRD-04 — {@code secondaryChannel} is validated against
 * {@code "WHATSAPP"}/{@code "SMS"} at the service boundary, matching the
 * public/plain-String-vs-internal-enum shape {@code DisputeTicketView}
 * already establishes for a package-private enum crossing the module
 * boundary (RULES.md §4.1).
 */
public record UpdateNotificationPreferenceCommand(String secondaryChannel) {
}
