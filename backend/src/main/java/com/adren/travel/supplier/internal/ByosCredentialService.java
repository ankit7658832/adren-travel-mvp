package com.adren.travel.supplier.internal;

import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.CurrentPrincipal;
import com.adren.travel.supplier.SupplierId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * FND-12 / PRD §10.4 — a Consultant's own BYOS supplier credentials.
 * {@code save} always scopes to the CALLING Consultant's own account
 * (never a client-supplied consultantId, mirroring
 * {@code WhitelabelServiceImpl#addUser}); {@code read} reuses FND-03's
 * {@link CurrentPrincipal#resolveTenantScope} check so Consultant B's
 * lookup of Consultant A's row by id is rejected the same way a
 * cross-tenant itinerary/booking lookup is.
 */
@Service
class ByosCredentialService {

    private final ByosCredentialRepository repository;
    private final KmsEnvelopeEncryptionService encryptionService;

    ByosCredentialService(ByosCredentialRepository repository, KmsEnvelopeEncryptionService encryptionService) {
        this.repository = repository;
        this.encryptionService = encryptionService;
    }

    @Transactional
    void save(SupplierId supplierId, String secretValue) {
        AdrenPrincipal principal = CurrentPrincipal.get();
        UUID consultantId = principal.consultantId();
        KmsEnvelopeEncryptionService.EncryptedPayload encrypted = encryptionService.encrypt(secretValue);

        ByosCredential credential = repository.findByConsultantIdAndSupplierId(consultantId, supplierId)
            .map(existing -> {
                existing.rotate(encrypted.ciphertext(), encrypted.iv(), encrypted.wrappedDataKey(), principal.userId());
                return existing;
            })
            .orElseGet(() -> new ByosCredential(UUID.randomUUID(), consultantId, supplierId,
                encrypted.ciphertext(), encrypted.iv(), encrypted.wrappedDataKey(), principal.userId()));
        repository.save(credential);
    }

    /**
     * @throws org.springframework.security.access.AccessDeniedException if
     *         {@code consultantId} isn't the calling principal's own tenant
     *         (RULES.md §5.2) — rejected before the row lookup even runs.
     */
    String read(UUID consultantId, SupplierId supplierId) {
        UUID scopedConsultantId = CurrentPrincipal.resolveTenantScope(consultantId);
        ByosCredential credential = repository.findByConsultantIdAndSupplierId(scopedConsultantId, supplierId)
            .orElseThrow(() -> new IllegalArgumentException(
                "No BYOS credential for consultant " + scopedConsultantId + " / supplier " + supplierId));
        return encryptionService.decrypt(
            new KmsEnvelopeEncryptionService.EncryptedPayload(
                credential.getCiphertext(), credential.getIv(), credential.getWrappedDataKey()));
    }
}
