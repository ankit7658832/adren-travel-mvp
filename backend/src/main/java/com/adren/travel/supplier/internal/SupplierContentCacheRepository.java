package com.adren.travel.supplier.internal;

import com.adren.travel.supplier.SupplierId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SupplierContentCacheRepository extends JpaRepository<SupplierContentCache, UUID> {

    Optional<SupplierContentCache> findBySupplierIdAndSupplierContentId(SupplierId supplierId, String supplierContentId);
}
