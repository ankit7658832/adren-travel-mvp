package com.adren.travel.payments.internal;

/**
 * Per PRD §20.12's Wallet Ledger Entry data dictionary (TopUp/Hold/Debit/
 * Refund/CommissionDeduction). {@code RELEASE} extends that illustrative
 * list — FIN-07's own AC requires a hold that resolves back to available
 * balance (not a debit) to leave its own auditable record (RULES.md §4.4),
 * and PRD §20.12 doesn't name a type for that outcome.
 */
enum LedgerEntryType {
    TOP_UP,
    HOLD,
    DEBIT,
    REFUND,
    COMMISSION_DEDUCTION,
    RELEASE
}
