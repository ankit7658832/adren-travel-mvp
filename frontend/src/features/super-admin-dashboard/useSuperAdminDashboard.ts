import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export interface CurrencyAmount {
  currency: string;
  amount: number;
}

export interface SupplierPerformance {
  supplierId: string;
  lineItemCount: number;
}

export interface AiGovernanceSummary {
  totalSuggestions: number;
  suggestedCount: number;
  noViableSuggestionCount: number;
  groqErrorCount: number;
}

export interface SuperAdminDashboardData {
  gmv: { gmvByCurrency: CurrencyAmount[] };
  supplierPerformance: SupplierPerformance[];
  aiGovernanceSummary: AiGovernanceSummary;
  adSpend: { spendByCurrency: CurrencyAmount[] };
}

/** HRD-11, PRD §9.5/§21.6 — the Super Admin Dashboard / Global Reporting composite read. */
export function useSuperAdminDashboard() {
  return useQuery({
    queryKey: ["super-admin-dashboard"],
    queryFn: async () => {
      const { data } = await apiClient.get<SuperAdminDashboardData>("/dashboard/super-admin");
      return data;
    },
  });
}
