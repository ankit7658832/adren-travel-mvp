package com.adren.travel.supplier.internal;

import com.adren.travel.supplier.ByosCredentialSummary;
import com.adren.travel.supplier.SaveByosCredentialCommand;
import com.adren.travel.supplier.SupplierSearchApi;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * HTTP surface for a Consultant's own BYOS supplier credentials (PRD §10.4,
 * DMC-06) — thin, all logic lives behind {@link SupplierSearchApi}. The
 * {@code consultantId} path segment matches the story's own resource-shaped
 * URL but is deliberately UNUSED here — {@code saveByosCredential}/
 * {@code findByosCredentials} always resolve the real scope from
 * {@code CurrentPrincipal}, never a path/body-supplied value (RULES.md
 * §5.2). This is a deliberate choice, not an oversight: this scaffold has
 * no login/session story yet, so the frontend has no session-derived
 * consultantId to put in the path — same "my own, always, the path segment
 * is a URL-shape artifact only" reasoning as
 * {@code LocalDmcService#findLocalDmcs}.
 */
@RestController
@RequestMapping("/api/v1/consultants/{consultantId}/byos-credentials")
class ByosCredentialController {

    private final SupplierSearchApi supplierSearchApi;

    ByosCredentialController(SupplierSearchApi supplierSearchApi) {
        this.supplierSearchApi = supplierSearchApi;
    }

    @PostMapping
    void save(@PathVariable UUID consultantId, @Valid @RequestBody SaveByosCredentialRequest request) {
        supplierSearchApi.saveByosCredential(new SaveByosCredentialCommand(request.supplierId(), request.secretValue()));
    }

    @GetMapping
    List<ByosCredentialSummary> findAll(@PathVariable UUID consultantId) {
        return supplierSearchApi.findByosCredentials();
    }
}
