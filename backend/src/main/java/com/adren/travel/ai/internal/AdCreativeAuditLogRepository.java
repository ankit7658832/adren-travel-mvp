package com.adren.travel.ai.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface AdCreativeAuditLogRepository extends JpaRepository<AdCreativeAuditLog, UUID> {

    List<AdCreativeAuditLog> findByPackageIdOrderByCreatedAt(UUID packageId);
}
