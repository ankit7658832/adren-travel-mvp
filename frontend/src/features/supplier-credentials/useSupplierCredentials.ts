import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export const SUPPLIER_IDS = ["HOTELBEDS", "STUBA", "TBO", "MYSTIFLY", "TRANSFERZ", "WIDGETY", "HBACTIVITIES"] as const;
export type SupplierId = (typeof SUPPLIER_IDS)[number];

export interface SupplierCredentialSummary {
  supplierId: SupplierId;
  configured: boolean;
  lastModifiedByUserId: string;
  lastModifiedAt: string;
}

const CREDENTIALS_QUERY_KEY = ["supplier-credentials"];

/** PRD §21.6 — masked supplier credential summaries (FND-10). Never the raw secret. */
export function useSupplierCredentials() {
  return useQuery({
    queryKey: CREDENTIALS_QUERY_KEY,
    queryFn: async () => {
      const { data } = await apiClient.get<SupplierCredentialSummary[]>("/suppliers/credentials");
      return data;
    },
  });
}

export function useUpdateSupplierCredential() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: { supplierId: SupplierId; secretValue: string }) => {
      await apiClient.put(`/suppliers/${input.supplierId}/credentials`, { secretValue: input.secretValue });
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CREDENTIALS_QUERY_KEY }),
  });
}
