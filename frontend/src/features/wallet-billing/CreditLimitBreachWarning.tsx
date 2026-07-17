import { useWallet, wouldBreachCreditLimit } from "./useWalletBilling";

export interface CreditLimitBreachWarningProps {
  /** The pending booking's total, in the wallet's own currency — see {@link wouldBreachCreditLimit}. */
  pendingAmount: number;
}

/**
 * PRD §21.7's pre-payment credit-limit breach warning (FIN-09) — "an
 * inline warning appears before they reach payment, not after." A
 * reusable component rather than logic baked into the payment step
 * itself, so any pre-payment screen with a known pending total can
 * compose it (Package Builder's publish step does today; a future
 * checkout/payment-flow screen, BOK-13, would too).
 * <p>
 * Renders nothing while the wallet is loading, on a fetch error, or when
 * there's no breach — this is a supplementary warning, not a blocking
 * gate, so it fails silent rather than surfacing its own error state on
 * top of whatever screen composes it.
 */
export function CreditLimitBreachWarning({ pendingAmount }: CreditLimitBreachWarningProps) {
  const walletQuery = useWallet();

  if (!walletQuery.isSuccess || !wouldBreachCreditLimit(walletQuery.data, pendingAmount)) {
    return null;
  }

  return (
    <div
      role="alert"
      className="rounded-md border border-warning-500/40 bg-warning-50 px-4 py-3"
    >
      <p className="text-sm font-medium text-neutral-900">This booking would exceed your credit limit</p>
      <p className="mt-1 text-sm text-neutral-700">
        {walletQuery.data.currency} {pendingAmount.toFixed(2)} is more than your available balance plus credit
        limit can currently cover. You can still continue, but payment will be blocked until your wallet has
        enough headroom.
      </p>
    </div>
  );
}
