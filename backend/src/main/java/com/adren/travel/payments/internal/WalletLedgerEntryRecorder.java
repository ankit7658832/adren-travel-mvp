package com.adren.travel.payments.internal;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * FIN-10's idempotency guarantee for concurrent writers: {@code
 * PROPAGATION_REQUIRES_NEW} runs the insert attempt in its OWN transaction,
 * so a caught {@link DataIntegrityViolationException} (the unique
 * constraint on {@code (related_booking_id, type)} tripping) only rolls
 * back this nested transaction, not the caller's — a plain try/catch
 * around a same-transaction insert would instead mark the WHOLE calling
 * transaction rollback-only, defeating the "no-op, not an error" contract.
 * <p>
 * This has to be a SEPARATE bean, not a private method on {@code
 * PaymentsServiceImpl}: {@code @Transactional} propagation only takes
 * effect through the Spring AOP proxy, and a same-class call bypasses it
 * entirely (a well-known Spring self-invocation pitfall).
 */
@Component
class WalletLedgerEntryRecorder {

    private final WalletLedgerEntryRepository walletLedgerEntryRepository;

    WalletLedgerEntryRecorder(WalletLedgerEntryRepository walletLedgerEntryRepository) {
        this.walletLedgerEntryRepository = walletLedgerEntryRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    boolean tryRecord(WalletLedgerEntry entry) {
        try {
            walletLedgerEntryRepository.saveAndFlush(entry);
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }
}
