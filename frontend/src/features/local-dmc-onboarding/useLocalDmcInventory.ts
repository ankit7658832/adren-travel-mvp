import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export interface LocalDmcInventoryItemView {
  itemId: string;
  localDmcId: string;
  productName: string;
  category: string;
  netRate: number;
  netRateCurrency: string;
  cancellationPolicyText: string;
  availableFrom: string;
  availableTo: string;
  updatedAt: string;
}

interface LocalDmcInventoryPageResponse {
  content: LocalDmcInventoryItemView[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface UpdateLocalDmcInventoryItemInput {
  itemId: string;
  productName: string;
  category: string;
  netRate: number;
  netRateCurrency: string;
  cancellationPolicyText: string;
  availableFrom: string;
  availableTo: string;
}

function inventoryQueryKey(localDmcId: string) {
  return ["local-dmc-inventory", localDmcId];
}

/**
 * PRD §10.2.8, DMC-10 — browse and edit a Local DMC's already-uploaded
 * inventory items. Shares the same query key ({@code local-dmc-inventory})
 * {@code useLocalDmcBulkUpload} already invalidates on a successful upload,
 * so a fresh bulk-upload is reflected here without extra wiring.
 */
export function useLocalDmcInventory(localDmcId: string) {
  const queryClient = useQueryClient();

  const listQuery = useQuery({
    queryKey: inventoryQueryKey(localDmcId),
    queryFn: async () => {
      const { data } = await apiClient.get<LocalDmcInventoryPageResponse>(
        `/local-dmc/${localDmcId}/inventory`,
        { params: { size: 50 } }
      );
      return data;
    },
    enabled: Boolean(localDmcId),
  });

  const update = useMutation({
    mutationFn: async (input: UpdateLocalDmcInventoryItemInput) => {
      const { itemId, ...body } = input;
      await apiClient.patch(`/local-dmc/${localDmcId}/inventory/${itemId}`, body);
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: inventoryQueryKey(localDmcId) }),
  });

  return { listQuery, update };
}
