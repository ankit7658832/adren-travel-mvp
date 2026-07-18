import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export const BYOS_SUPPLIER_IDS = ["HOTELBEDS", "STUBA", "TBO", "MYSTIFLY", "TRANSFERZ", "WIDGETY", "HBACTIVITIES"] as const;
export type ByosSupplierId = (typeof BYOS_SUPPLIER_IDS)[number];

export interface ByosCredentialSummary {
  supplierId: ByosSupplierId;
  configured: boolean;
  lastModifiedAt: string;
}

const BYOS_CREDENTIALS_QUERY_KEY = ["byos-credentials"];

/**
 * The `{consultantId}` path segment `POST/GET /api/v1/consultants/{id}/byos-credentials`
 * expects is never actually used server-side to scope the request — the
 * backend always resolves the real tenant from the caller's own JWT
 * (RULES.md §5.2), the same "path segment is a URL-shape artifact only"
 * reasoning as {@code LocalDmcService#findLocalDmcs}. This scaffold has no
 * login/session story yet, so there is no real consultantId to put here —
 * any value works identically.
 */
const PATH_PLACEHOLDER_CONSULTANT_ID = "00000000-0000-0000-0000-000000000000";

/** PRD §10.4, DMC-06 — a Consultant's own BYOS supplier credentials. Never the raw secret value, only a masked summary. */
export function useByosCredentialEntry() {
  const queryClient = useQueryClient();

  const listQuery = useQuery({
    queryKey: BYOS_CREDENTIALS_QUERY_KEY,
    queryFn: async () => {
      const { data } = await apiClient.get<ByosCredentialSummary[]>(
        `/consultants/${PATH_PLACEHOLDER_CONSULTANT_ID}/byos-credentials`
      );
      return data;
    },
  });

  const save = useMutation({
    mutationFn: async (input: { supplierId: ByosSupplierId; secretValue: string }) => {
      await apiClient.post(`/consultants/${PATH_PLACEHOLDER_CONSULTANT_ID}/byos-credentials`, input);
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: BYOS_CREDENTIALS_QUERY_KEY }),
  });

  return { listQuery, save };
}
