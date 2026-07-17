import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export type LedgerEntryType =
  | "TOP_UP"
  | "HOLD"
  | "DEBIT"
  | "REFUND"
  | "COMMISSION_DEDUCTION"
  | "RELEASE"
  | "ON_ACCOUNT";

export const LEDGER_ENTRY_TYPES: LedgerEntryType[] = [
  "TOP_UP",
  "HOLD",
  "DEBIT",
  "REFUND",
  "COMMISSION_DEDUCTION",
  "RELEASE",
  "ON_ACCOUNT",
];

export interface WalletSummary {
  consultantId: string;
  availableBalance: string;
  creditLimit: string;
  pendingHolds: string;
  currency: string;
  updatedAt: string;
}

export interface WalletLedgerEntry {
  ledgerEntryId: string;
  consultantId: string;
  type: LedgerEntryType;
  amount: string;
  currency: string;
  relatedBookingId: string | null;
  balanceAfter: string;
  createdAt: string;
}

interface WalletLedgerPageResponse {
  content: WalletLedgerEntry[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

const WALLET_QUERY_KEY = ["wallet"];

/** PRD §12.3/§21.7 — a Consultant's wallet balance/credit-limit/pending-holds summary (FIN-06/FIN-09). Server data, so React Query. */
export function useWallet() {
  return useQuery({
    queryKey: WALLET_QUERY_KEY,
    queryFn: async () => {
      const { data } = await apiClient.get<WalletSummary>("/wallet");
      return data;
    },
  });
}

/**
 * PRD §12.3 — a booking would breach the credit limit if the pending
 * amount exceeds what's left of availableBalance + creditLimit once
 * pendingHolds are accounted for (FIN-08's own wallet-side check,
 * mirrored here client-side for the pre-payment warning FIN-09 requires
 * — the backend's `chk_wallet_within_credit_limit` constraint remains the
 * actual enforcement; this is the UX fast-path/early-warning layer only).
 */
export function wouldBreachCreditLimit(wallet: WalletSummary, pendingAmount: number): boolean {
  const headroom = Number(wallet.availableBalance) + Number(wallet.creditLimit) - Number(wallet.pendingHolds);
  return pendingAmount > headroom;
}

/**
 * PRD §21.7 — Wallet & Billing screen (FIN-09): balance summary plus a
 * transaction ledger filterable by `WalletLedgerEntry.type`. `typeFilter`
 * of `null` means unfiltered (matches the backend's `type=` query param
 * being optional).
 */
export function useWalletBilling() {
  const walletQuery = useWallet();
  const [typeFilter, setTypeFilter] = useState<LedgerEntryType | null>(null);

  const ledgerQuery = useQuery({
    queryKey: ["wallet-ledger", typeFilter],
    queryFn: async () => {
      const { data } = await apiClient.get<WalletLedgerPageResponse>("/wallet/ledger", {
        params: typeFilter ? { type: typeFilter } : {},
      });
      return data;
    },
  });

  return { walletQuery, ledgerQuery, typeFilter, setTypeFilter };
}
