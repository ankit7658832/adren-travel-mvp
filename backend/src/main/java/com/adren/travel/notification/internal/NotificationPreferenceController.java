package com.adren.travel.notification.internal;

import com.adren.travel.notification.NotificationApi;
import com.adren.travel.notification.NotificationPreferenceView;
import com.adren.travel.notification.UpdateNotificationPreferenceCommand;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * HTTP surface for HRD-04's Notification Preferences screen — thin, all
 * logic lives behind {@link NotificationApi}. The {@code consultantId}
 * path segment is unused server-side, same reasoning as {@code
 * ByosCredentialController}'s Javadoc.
 */
@RestController
@RequestMapping("/api/v1/consultants/{consultantId}/notification-preference")
class NotificationPreferenceController {

    private final NotificationApi notificationApi;

    NotificationPreferenceController(NotificationApi notificationApi) {
        this.notificationApi = notificationApi;
    }

    @PutMapping
    void update(@PathVariable UUID consultantId, @Valid @RequestBody UpdateNotificationPreferenceRequest request) {
        notificationApi.updateNotificationPreference(new UpdateNotificationPreferenceCommand(request.secondaryChannel()));
    }

    @GetMapping
    NotificationPreferenceView find(@PathVariable UUID consultantId) {
        return notificationApi.findNotificationPreference();
    }
}
