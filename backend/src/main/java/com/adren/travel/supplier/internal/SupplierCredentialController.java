package com.adren.travel.supplier.internal;

import com.adren.travel.supplier.SupplierCredentialSummary;
import com.adren.travel.supplier.SupplierId;
import com.adren.travel.supplier.SupplierSearchApi;
import com.adren.travel.supplier.UpdateSupplierCredentialCommand;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * PRD §21.6 — Super Admin's Adren-owned supplier credential management
 * (FND-10). Controller depends on {@link SupplierSearchApi} only.
 */
@RestController
@RequestMapping("/api/v1/suppliers")
class SupplierCredentialController {

    private final SupplierSearchApi supplierSearchApi;

    SupplierCredentialController(SupplierSearchApi supplierSearchApi) {
        this.supplierSearchApi = supplierSearchApi;
    }

    @PutMapping("/{supplierId}/credentials")
    void updateCredential(@PathVariable SupplierId supplierId, @Valid @RequestBody UpdateSupplierCredentialRequest request) {
        supplierSearchApi.updateSupplierCredential(new UpdateSupplierCredentialCommand(supplierId, request.secretValue()));
    }

    @GetMapping("/credentials")
    List<SupplierCredentialSummary> listCredentials() {
        return supplierSearchApi.listSupplierCredentials();
    }
}
