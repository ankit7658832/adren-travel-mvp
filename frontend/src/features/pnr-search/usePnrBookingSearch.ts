import { useMutation } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export interface BookingSearchResultView {
  bookingId: string;
  pnrSearchableRef: string;
  status: string;
  totalSellPrice: { amount: string; currency: string };
  paymentMethod: string;
  createdAt: string;
}

interface BookingSearchPageResponse {
  content: BookingSearchResultView[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/**
 * PRD §16, §21.9, HRD-08 — a single reference finds a booking summary
 * regardless of product type (HRD-07's own AC — {@code Booking} carries
 * no product-type field to filter on). A mutation, not a query: search
 * only runs on explicit submission, matching {@code useMultiLocationSearch}'s
 * own "search is a user action, not something to auto-fetch" shape.
 */
export function usePnrBookingSearch() {
  return useMutation({
    mutationFn: async (ref: string) => {
      const { data } = await apiClient.get<BookingSearchPageResponse>("/bookings/search", { params: { ref } });
      return data.content;
    },
  });
}
