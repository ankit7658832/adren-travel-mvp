import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export const SECONDARY_CHANNELS = ["WHATSAPP", "SMS"] as const;
export type SecondaryChannel = (typeof SECONDARY_CHANNELS)[number];

export interface NotificationPreferenceView {
  secondaryChannel: SecondaryChannel;
  isOverride: boolean;
}

/**
 * The {@code {consultantId}} path segment `GET/PUT
 * /api/v1/consultants/{id}/notification-preference` expects is never
 * actually used server-side to scope the request — the backend always
 * resolves the real tenant from the caller's own JWT (RULES.md §5.2), the
 * same "path segment is a URL-shape artifact only" reasoning as {@code
 * useByosCredentialEntry}. This scaffold has no login/session story yet,
 * so there is no real consultantId to put here — any value works identically.
 */
const PATH_PLACEHOLDER_CONSULTANT_ID = "00000000-0000-0000-0000-000000000000";

const NOTIFICATION_PREFERENCE_QUERY_KEY = ["notification-preference"];

/** PRD §21.10, HRD-04 — a Consultant's own secondary notification channel, defaulting to their region's default. */
export function useNotificationPreferences() {
  const queryClient = useQueryClient();

  const preferenceQuery = useQuery({
    queryKey: NOTIFICATION_PREFERENCE_QUERY_KEY,
    queryFn: async () => {
      const { data } = await apiClient.get<NotificationPreferenceView>(
        `/consultants/${PATH_PLACEHOLDER_CONSULTANT_ID}/notification-preference`
      );
      return data;
    },
  });

  const save = useMutation({
    mutationFn: async (secondaryChannel: SecondaryChannel) => {
      await apiClient.put(`/consultants/${PATH_PLACEHOLDER_CONSULTANT_ID}/notification-preference`, { secondaryChannel });
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: NOTIFICATION_PREFERENCE_QUERY_KEY }),
  });

  return { preferenceQuery, save };
}
