package com.adren.travel.notification;

/**
 * PRD §21.10, HRD-04 — the CALLING Consultant's effective secondary
 * channel: their own saved override if one exists, otherwise the regional
 * default {@code isOverride=false} signals which one is showing, so the
 * screen can pre-select it without the frontend re-deriving the market
 * default itself.
 */
public record NotificationPreferenceView(String secondaryChannel, boolean isOverride) {
}
