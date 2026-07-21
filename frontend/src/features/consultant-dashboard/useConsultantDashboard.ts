import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export interface ConsultantBookingMetrics {
  bookingsThisMonth: number;
  gmvThisMonth: { amount: number; currency: string };
}

export interface WalletSummary {
  consultantId: string;
  availableBalance: number;
  creditLimit: number;
  pendingHolds: number;
  currency: string;
  updatedAt: string;
}

export interface PackageSummary {
  packageId: string;
  name: string;
  bookingCount: number;
}

export interface QuotationSummary {
  itineraryId: string;
  createdAt: string;
}

export interface ConsultantDashboardData {
  metrics: ConsultantBookingMetrics;
  wallet: WalletSummary;
  topPackages: PackageSummary[];
  pendingQuotations: QuotationSummary[];
  activeCampaigns: unknown[];
}

/** HRD-09, PRD §9.5/§21.5 — the Consultant Dashboard's composite read. */
export function useConsultantDashboard() {
  return useQuery({
    queryKey: ["consultant-dashboard"],
    queryFn: async () => {
      const { data } = await apiClient.get<ConsultantDashboardData>("/dashboard/consultant");
      return data;
    },
  });
}
