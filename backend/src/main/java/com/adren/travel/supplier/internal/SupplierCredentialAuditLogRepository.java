package com.adren.travel.supplier.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SupplierCredentialAuditLogRepository extends JpaRepository<SupplierCredentialAuditLog, UUID> {
}
