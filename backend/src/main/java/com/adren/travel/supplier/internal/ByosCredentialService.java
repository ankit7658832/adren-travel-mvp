package com.adren.travel.supplier.internal;

import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.CurrentPrincipal;
import com.adren.travel.supplier.ByosCredentialSummary;
import com.adren.travel.supplier.SupplierId;
import com.adren.travel.supplier.event.ByosCredentialSavedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FND-12 / PRD §10.4 — a Consultant's own BYOS supplier credentials.
 * {@code save} always scopes to the CALLING Consultant's own account
 * (never a client-supplied consultantId, mirroring
 * {@code WhitelabelServiceImpl#addUser}); {@code read} reuses FND-03's
 * {@link CurrentPrincipal#resolveTenantScope} check so Consultant B's
 * lookup of Consultant A's row by id is rejected the same way a
 * cross-tenant itinerary/booking lookup is. {@link #readForCurrentConsultant}
 * (DMC-07/09) goes further still — it takes no consultantId parameter at
 * all, so there is no IDOR surface to check in the first place, the
 * strongest possible form of tenant isolation for the credential-resolution
 * path a search request runs through.
 */
@Service
class ByosCredentialService {

    private final ByosCredentialRepository repository;
    private final KmsEnvelopeEncryptionService encryptionService;
    private final ApplicationEventPublisher events;

    ByosCredentialService(ByosCredentialRepository repository, KmsEnvelopeEncryptionService encryptionService,
                          ApplicationEventPublisher events) {
        this.repository = repository;
        this.encryptionService = encryptionService;
        this.events = events;
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
        events.publishEvent(new ByosCredentialSavedEvent(consultantId, supplierId));
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
        return decrypt(credential);
    }

    /**
     * DMC-07/09 — resolves a BYOS credential for whichever tenant the
     * CURRENT request belongs to, with no consultantId parameter to spoof
     * or mis-scope. Empty (never an exception) when the calling principal
     * has no BYOS credential for this supplier — the correct "fall through
     * to Adren's own credential" signal for {@code SupplierCredentialResolver}.
     * Empty for a Super Admin (no consultant tenant of their own) too.
     */
    Optional<String> readForCurrentConsultant(SupplierId supplierId) {
        UUID consultantId = CurrentPrincipal.get().consultantId();
        if (consultantId == null) {
            return Optional.empty();
        }
        return repository.findByConsultantIdAndSupplierId(consultantId, supplierId).map(this::decrypt);
    }

    /** Every supplier the calling Consultant has BYOS-configured — masked, never the secret value. */
    List<ByosCredentialSummary> findByosCredentialsForCurrentConsultant() {
        UUID consultantId = CurrentPrincipal.get().consultantId();
        if (consultantId == null) {
            return List.of();
        }
        return repository.findByConsultantId(consultantId).stream()
            .map(c -> new ByosCredentialSummary(c.getSupplierId(), true, c.getLastModifiedAt()))
            .toList();
    }

    private String decrypt(ByosCredential credential) {
        return encryptionService.decrypt(
            new KmsEnvelopeEncryptionService.EncryptedPayload(
                credential.getCiphertext(), credential.getIv(), credential.getWrappedDataKey()));
    }
}
