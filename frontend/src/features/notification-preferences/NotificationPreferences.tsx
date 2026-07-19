import { useEffect, useState, type FormEvent } from "react";
import { Badge } from "@/shared/design-system/Badge";
import { Button } from "@/shared/design-system/Button";
import {
  SECONDARY_CHANNELS,
  useNotificationPreferences,
  type SecondaryChannel,
} from "./useNotificationPreferences";

/**
 * PRD §21.10, HRD-04 — a Consultant's own secondary notification channel:
 * the regional default is pre-selected, and the Consultant can override
 * it (PRD §15/§22.7 T11). Default/loading/success/error states; empty is
 * N/A here — there is always exactly one effective channel to show
 * (override or regional default), never a zero-items case.
 */
export function NotificationPreferences() {
  const { preferenceQuery, save } = useNotificationPreferences();
  const [selectedChannel, setSelectedChannel] = useState<SecondaryChannel | null>(null);

  useEffect(() => {
    if (preferenceQuery.isSuccess) {
      setSelectedChannel(preferenceQuery.data.secondaryChannel);
    }
  }, [preferenceQuery.isSuccess, preferenceQuery.data]);

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (selectedChannel) {
      save.mutate(selectedChannel);
    }
  }

  return (
    <main className="mx-auto max-w-3xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">Notification Preferences</h1>
      <p className="mt-1 text-sm text-neutral-600">
        Email notifications are always sent. Choose which secondary channel you'd also like to receive them on.
      </p>

      {preferenceQuery.isLoading && (
        <p role="status" className="mt-6 text-sm text-neutral-600">
          Loading your notification preferences…
        </p>
      )}

      {preferenceQuery.isError && (
        <div
          role="alert"
          className="mt-6 flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3"
        >
          <p className="text-sm text-error-700">Could not load your notification preferences.</p>
          <Button variant="secondary" size="sm" onClick={() => preferenceQuery.refetch()}>
            Retry
          </Button>
        </div>
      )}

      {preferenceQuery.isSuccess && selectedChannel && (
        <form onSubmit={handleSubmit} className="mt-6 space-y-3 rounded-md border border-neutral-200 bg-surface p-4">
          <div className="flex items-center gap-2">
            <label htmlFor="secondary-channel-select" className="block text-sm font-medium text-neutral-700">
              Secondary channel
            </label>
            {preferenceQuery.data.isOverride ? (
              <Badge tone="info">Custom</Badge>
            ) : (
              <Badge tone="neutral">Regional default</Badge>
            )}
          </div>
          <select
            id="secondary-channel-select"
            value={selectedChannel}
            onChange={(e) => setSelectedChannel(e.target.value as SecondaryChannel)}
            className="h-10 w-full rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-focus-ring focus-visible:ring-offset-2"
          >
            {SECONDARY_CHANNELS.map((channel) => (
              <option key={channel} value={channel}>
                {channel === "WHATSAPP" ? "WhatsApp" : "SMS"}
              </option>
            ))}
          </select>
          <Button type="submit" disabled={save.isPending}>
            {save.isPending ? "Saving…" : "Save"}
          </Button>
          {save.isError && (
            <p role="alert" className="text-sm text-error-700">
              Could not save this preference. Please try again.
            </p>
          )}
          {save.isSuccess && (
            <p role="status" className="text-sm text-success-700">
              Preference saved.
            </p>
          )}
        </form>
      )}
    </main>
  );
}
