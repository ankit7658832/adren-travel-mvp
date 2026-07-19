package com.adren.travel.notification;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Public API of the Notification module (PRD §15/§21.10, HRD-04) — the
 * module's first real public surface beyond being an event consumer. Both
 * methods always resolve the CALLING Consultant's own tenant from {@code
 * CurrentPrincipal}, never a client-supplied consultantId (RULES.md §5.2)
 * — same "path segment is a URL-shape artifact only" reasoning as {@code
 * SupplierSearchApi#saveByosCredential}, since this scaffold still has no
 * login/session story for the frontend to derive a real consultantId from.
 * {@code CONSULTANT}-only: a notification channel preference is a
 * per-tenant setting, not something Super Admin manages on a Consultant's
 * behalf, and Users don't have their own notification routing (HRD-01's
 * dispatch is always Consultant-scoped).
 */
public interface NotificationApi {

    @PreAuthorize("hasRole('CONSULTANT')")
    void updateNotificationPreference(UpdateNotificationPreferenceCommand command);

    @PreAuthorize("hasRole('CONSULTANT')")
    NotificationPreferenceView findNotificationPreference();
}
