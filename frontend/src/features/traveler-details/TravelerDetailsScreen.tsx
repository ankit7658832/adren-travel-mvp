import { useState } from "react";
import { CheckCircle2 } from "lucide-react";
import { Button } from "@/shared/design-system/Button";
import { TextField } from "@/shared/design-system/TextField";
import { Card } from "@/shared/design-system/Card";
import {
  emptyTraveler,
  isTravelerComplete,
  useCreateTravelerProfile,
  type TravelerEntry,
} from "./useTravelerDetails";

/**
 * SCR-14 (doc/ADREN_UIUX_SPEC.md §9) — Pax/Traveler Details as its own
 * screen, backed by the same already-real `POST /api/v1/travelers`
 * {@link import("../booking-payment-flow/BookingPaymentFlow").BookingPaymentFlow}'s
 * inline traveler step uses. Passport fields are conditional on a
 * "requires documents" toggle per traveler rather than an itinerary-
 * derived signal (per spec, "only when the itinerary contains a cruise
 * or international product") — this screen isn't itinerary-scoped, so
 * there's no such signal to read; the toggle is the honest equivalent.
 */
export function TravelerDetailsScreen() {
  const [travelers, setTravelers] = useState<TravelerEntry[]>([emptyTraveler(crypto.randomUUID())]);
  const [savedIds, setSavedIds] = useState<string[]>([]);
  const createTravelerProfile = useCreateTravelerProfile();

  function updateTraveler(key: string, patch: Partial<TravelerEntry>) {
    setTravelers((prev) => prev.map((t) => (t.key === key ? { ...t, ...patch } : t)));
  }

  function addTraveler() {
    setTravelers((prev) => [...prev, emptyTraveler(crypto.randomUUID())]);
  }

  async function handleSubmit() {
    try {
      const ids: string[] = [];
      for (const traveler of travelers) {
        const result = await createTravelerProfile.mutateAsync(traveler);
        ids.push(result.travelerId);
      }
      setSavedIds(ids);
    } catch {
      // createTravelerProfile.isError already reflects this; nothing
      // further to do here beyond not letting it become an unhandled rejection.
    }
  }

  const allComplete = travelers.every(isTravelerComplete);

  if (savedIds.length > 0) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-8">
        <div role="status" className="rounded-md border border-success-700/20 bg-success-50 px-4 py-3 text-success-700">
          Saved {savedIds.length} traveler{savedIds.length === 1 ? "" : "s"}.
        </div>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-2xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">Traveler details</h1>

      <div className="mt-6 space-y-4">
        {travelers.map((traveler, index) => (
          <Card key={traveler.key}>
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-medium text-neutral-900">Traveler {index + 1}</h2>
              {isTravelerComplete(traveler) && (
                <CheckCircle2 aria-label="complete" className="h-5 w-5 text-success-500" />
              )}
            </div>

            <div className="mt-4 space-y-4">
              <TextField
                label="Full name"
                required
                value={traveler.name}
                onChange={(e) => updateTraveler(traveler.key, { name: e.target.value })}
              />
              <TextField
                label="Date of birth"
                type="date"
                required
                value={traveler.dateOfBirth}
                onChange={(e) => updateTraveler(traveler.key, { dateOfBirth: e.target.value })}
              />
              <TextField
                label="Nationality"
                value={traveler.nationality}
                onChange={(e) => updateTraveler(traveler.key, { nationality: e.target.value })}
              />

              <label className="flex items-center gap-2 text-sm text-neutral-900">
                <input
                  type="checkbox"
                  checked={traveler.requiresDocuments}
                  onChange={(e) => updateTraveler(traveler.key, { requiresDocuments: e.target.checked })}
                />
                This trip includes international travel or a cruise
              </label>

              {traveler.requiresDocuments && (
                <>
                  <TextField
                    label="Passport number"
                    required
                    value={traveler.passportNumber}
                    onChange={(e) => updateTraveler(traveler.key, { passportNumber: e.target.value })}
                  />
                  <TextField
                    label="Passport expiry"
                    type="date"
                    required
                    value={traveler.passportExpiry}
                    onChange={(e) => updateTraveler(traveler.key, { passportExpiry: e.target.value })}
                  />
                </>
              )}
            </div>
          </Card>
        ))}
      </div>

      <Button variant="ghost" onClick={addTraveler} className="mt-4">
        + Add Traveler
      </Button>

      {createTravelerProfile.isError && (
        <p role="alert" className="mt-4 text-sm text-error-700">
          Could not save traveler details. Please try again.
        </p>
      )}

      <Button
        onClick={handleSubmit}
        disabled={!allComplete || createTravelerProfile.isPending}
        className="mt-6 w-full"
      >
        {createTravelerProfile.isPending ? "Saving…" : "Save traveler details"}
      </Button>
    </main>
  );
}
