package com.adren.travel.notification.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {
}
