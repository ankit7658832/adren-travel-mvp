package com.adren.travel.supplier.internal;

import com.adren.travel.supplier.SupplierId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface ByosCredentialRepository extends JpaRepository<ByosCredential, UUID> {

    Optional<ByosCredential> findByConsultantIdAndSupplierId(UUID consultantId, SupplierId supplierId);
}
