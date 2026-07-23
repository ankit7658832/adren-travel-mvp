import { useState, type FormEvent } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Button } from "@/shared/design-system/Button";
import { TextField } from "@/shared/design-system/TextField";
import { Card } from "@/shared/design-system/Card";
import { CreditLimitBreachWarning } from "@/features/wallet-billing/CreditLimitBreachWarning";
import {
  usePackageDetails,
  useCreateTravelerProfile,
  useConfirmBooking,
  type ConfirmBookingInput,
} from "./useBookingPaymentFlow";

/**
 * PRD §9.1 Flow C, §21.4 — Booking & Payment Flow (HRD-15, completing
 * BOK-13's frontend). Traveler details → price breakdown (collapsible
 * net/markup) → payment method → confirm, composed from already-real
 * backend endpoints (BOK-13's `confirmBooking`/`confirmBookingOnAccount`,
 * BOK-14's traveler-profile capture, and HRD-15's own `GET
 * /api/v1/packages/{id}` — the same service method AI-12 already used
 * internally, exposed via REST for the first time here).
 */
export function BookingPaymentFlow() {
  const { packageId } = useParams<{ packageId: string }>();
  const navigate = useNavigate();
  const packageQuery = usePackageDetails(packageId);
  const createTravelerProfile = useCreateTravelerProfile();
  const confirmBooking = useConfirmBooking();

  const [name, setName] = useState("");
  const [dateOfBirth, setDateOfBirth] = useState("");
  const [passportNumber, setPassportNumber] = useState("");
  const [passportExpiry, setPassportExpiry] = useState("");
  const [nationality, setNationality] = useState("");
  const [paymentMethod, setPaymentMethod] = useState<ConfirmBookingInput["paymentMethod"]>("WALLET");

  if (packageQuery.isLoading) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-8">
        <p role="status" className="text-sm text-neutral-600">
          Loading package details…
        </p>
      </main>
    );
  }

  if (packageQuery.isError || !packageQuery.data) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-8">
        <div role="alert" className="flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3">
          <p className="text-sm text-error-700">Could not load this package.</p>
          <Button variant="secondary" size="sm" onClick={() => packageQuery.refetch()}>
            Retry
          </Button>
        </div>
      </main>
    );
  }

  const travelPackage = packageQuery.data;
  const totalSellPrice = (Number(travelPackage.basePrice) + Number(travelPackage.markupPrice)).toFixed(2);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    createTravelerProfile.mutate(
      {
        name,
        dateOfBirth,
        passportNumber: passportNumber || undefined,
        passportExpiry: passportExpiry || undefined,
        nationality: nationality || undefined,
      },
      {
        onSuccess: () => {
          confirmBooking.mutate(
            {
              quotationOrPackageId: travelPackage.packageId,
              totalSellPrice,
              currency: travelPackage.currency,
              paymentMethod,
            },
            {
              // SCR-18's own spec: "routes to SCR-17 on success" —
              // BookingConfirmation is that screen.
              onSuccess: (data) => navigate(`/bookings/${data.bookingId}/confirmation`),
            }
          );
        },
      }
    );
  }

  const isSubmitting = createTravelerProfile.isPending || confirmBooking.isPending;

  return (
    <main className="mx-auto max-w-2xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">{travelPackage.name}</h1>
      <p className="mt-1 text-sm text-neutral-600">{travelPackage.description}</p>

      <form onSubmit={handleSubmit} className="mt-6 space-y-6">
        <fieldset className="space-y-4">
          <legend className="text-lg font-medium text-neutral-900">Traveler details</legend>
          <TextField label="Full name" required value={name} onChange={(e) => setName(e.target.value)} />
          <TextField
            label="Date of birth"
            type="date"
            required
            value={dateOfBirth}
            onChange={(e) => setDateOfBirth(e.target.value)}
          />
          <TextField
            label="Passport number"
            value={passportNumber}
            onChange={(e) => setPassportNumber(e.target.value)}
          />
          <TextField
            label="Passport expiry"
            type="date"
            value={passportExpiry}
            onChange={(e) => setPassportExpiry(e.target.value)}
          />
          <TextField label="Nationality" value={nationality} onChange={(e) => setNationality(e.target.value)} />
        </fieldset>

        <Card>
          <h2 className="text-lg font-medium text-neutral-900">Price breakdown</h2>
          <details className="mt-2">
            <summary className="cursor-pointer text-sm font-medium text-neutral-700">
              Net {travelPackage.currency} {Number(travelPackage.basePrice).toFixed(2)} + markup{" "}
              {travelPackage.currency} {Number(travelPackage.markupPrice).toFixed(2)}
            </summary>
            <dl className="mt-2 space-y-1 text-sm text-neutral-600">
              <div className="flex justify-between">
                <dt>Net (supplier cost)</dt>
                <dd>
                  {travelPackage.currency} {Number(travelPackage.basePrice).toFixed(2)}
                </dd>
              </div>
              <div className="flex justify-between">
                <dt>Markup</dt>
                <dd>
                  {travelPackage.currency} {Number(travelPackage.markupPrice).toFixed(2)}
                </dd>
              </div>
            </dl>
          </details>
          <p className="mt-3 text-xl font-semibold text-neutral-900">
            Total: {travelPackage.currency} {totalSellPrice}
          </p>
        </Card>

        <CreditLimitBreachWarning pendingAmount={Number(totalSellPrice)} />

        <fieldset className="space-y-2">
          <legend className="text-lg font-medium text-neutral-900">Payment method</legend>
          <label className="flex items-center gap-2 text-sm text-neutral-900">
            <input
              type="radio"
              name="payment-method"
              value="WALLET"
              checked={paymentMethod === "WALLET"}
              onChange={() => setPaymentMethod("WALLET")}
            />
            Wallet
          </label>
          <label className="flex items-center gap-2 text-sm text-neutral-900">
            <input
              type="radio"
              name="payment-method"
              value="ON_ACCOUNT"
              checked={paymentMethod === "ON_ACCOUNT"}
              onChange={() => setPaymentMethod("ON_ACCOUNT")}
            />
            On-Account
          </label>
        </fieldset>

        {(createTravelerProfile.isError || confirmBooking.isError) && (
          <p role="alert" className="text-sm text-error-700">
            Could not complete this booking. Please try again.
          </p>
        )}

        <Button type="submit" disabled={isSubmitting} className="w-full">
          {isSubmitting ? "Confirming…" : "Confirm booking"}
        </Button>
      </form>
    </main>
  );
}
