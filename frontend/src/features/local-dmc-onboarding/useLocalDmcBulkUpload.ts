import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export interface LocalDmcInventoryRowError {
  rowNumber: number;
  fieldErrors: string[];
}

export interface LocalDmcInventoryUploadResult {
  successCount: number;
  errors: LocalDmcInventoryRowError[];
}

/**
 * PRD §10.2.8, DMC-03 — bulk-uploads a Local DMC's inventory catalogue
 * from a CSV. All-or-nothing on the backend: a non-empty {@code errors}
 * array means nothing was persisted, matching the story's own "not a
 * partial silent import" AC — this hook surfaces that result as-is rather
 * than treating a row-error response as an HTTP failure (it's a normal
 * 200, per {@code AiItineraryGenerationResult}'s own "explicit-state,
 * not-an-exception" precedent).
 */
export function useLocalDmcBulkUpload(localDmcId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (csvContent: string) => {
      const { data } = await apiClient.post<LocalDmcInventoryUploadResult>(
        `/local-dmc/${localDmcId}/inventory/bulk-upload`,
        { csvContent }
      );
      return data;
    },
    onSuccess: (result) => {
      if (result.errors.length === 0) {
        queryClient.invalidateQueries({ queryKey: ["local-dmc-inventory", localDmcId] });
      }
    },
  });
}
