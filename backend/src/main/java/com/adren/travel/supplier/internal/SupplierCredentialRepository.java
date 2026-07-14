package com.adren.travel.supplier.internal;

import com.adren.travel.supplier.SupplierId;
import org.springframework.data.jpa.repository.JpaRepository;

interface SupplierCredentialRepository extends JpaRepository<SupplierCredential, SupplierId> {
}
