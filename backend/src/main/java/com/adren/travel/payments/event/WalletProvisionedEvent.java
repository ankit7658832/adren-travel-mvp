package com.adren.travel.payments.event;

import java.util.UUID;

/**
 * Published the first time a Consultant's wallet is accessed and has to be
 * auto-provisioned with zero balance/credit-limit defaults (PRD §12.3,
 * FIN-06) — there is no separate "create wallet" story, so provisioning
 * happens lazily on first {@code getWallet} call.
 */
public record WalletProvisionedEvent(UUID consultantId) {
}
