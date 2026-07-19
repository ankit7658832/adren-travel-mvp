import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export interface DisputeTicketView {
  disputeTicketId: string;
  bookingId: string;
  reason: string;
  status: string;
  createdAt: string;
}

interface DisputeTicketPageResponse {
  content: DisputeTicketView[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/**
 * PRD §12.5, HRD-06 — a dispute is a trackable ticket with a status, not
 * just an emailed notice. {@code consultantId} is left unset — the
 * backend resolves the caller's own tenant from the JWT (`consultantId`
 * is an optional query param only Super Admin's "view all"/"view one"
 * path actually uses, per {@code BookingApi#findDisputeTickets}).
 */
export function useDisputeTickets() {
  return useQuery({
    queryKey: ["dispute-tickets"],
    queryFn: async () => {
      const { data } = await apiClient.get<DisputeTicketPageResponse>("/disputes", { params: { size: 50 } });
      return data;
    },
  });
}
