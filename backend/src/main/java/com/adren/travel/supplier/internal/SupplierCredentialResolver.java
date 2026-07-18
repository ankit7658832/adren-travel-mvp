package com.adren.travel.supplier.internal;

import com.adren.travel.supplier.SupplierId;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * DMC-07 / PRD §10.2.9 — the upstream credential-source resolver every
 * supplier client's caller invokes ONCE per search request: BYOS if the
 * current request's tenant has one configured for this supplier, else
 * Adren's own. This is a dependency-injection concern, not a branching
 * concern (backend-best-practices §6) — {@code HotelbedsClient} (and every
 * future supplier client) receives whichever value this resolver returns
 * as a plain parameter and never asks "is this BYOS or Adren's own?"
 * itself. Resolution order (BYOS first) IS the merge logic DMC-08's own
 * acceptance criterion needs — a Consultant with BYOS Hotelbeds credentials
 * configured gets their inventory in the exact same call, normalized the
 * exact same way, as every other Consultant's Adren-sourced Hotelbeds call.
 */
@Component
class SupplierCredentialResolver {

    private final ByosCredentialService byosCredentialService;
    private final SupplierCredentialRepository credentialRepository;
    private final SupplierSecretsService supplierSecretsService;

    SupplierCredentialResolver(ByosCredentialService byosCredentialService,
                               SupplierCredentialRepository credentialRepository,
                               SupplierSecretsService supplierSecretsService) {
        this.byosCredentialService = byosCredentialService;
        this.credentialRepository = credentialRepository;
        this.supplierSecretsService = supplierSecretsService;
    }

    /** @return the resolved credential value, or empty if neither a BYOS nor an Adren-owned credential is configured. */
    Optional<String> resolve(SupplierId supplierId) {
        Optional<String> byos = byosCredentialService.readForCurrentConsultant(supplierId);
        if (byos.isPresent()) {
            return byos;
        }
        return credentialRepository.findById(supplierId)
            .map(credential -> supplierSecretsService.getSecretValue(credential.getSecretArn()));
    }
}
