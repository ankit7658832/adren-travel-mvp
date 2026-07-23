import { useMutation, useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export interface VoucherView {
  pdfReference: string;
  atolCertificateReference: string | null;
  generatedAt: string;
}

export interface BookingView {
  bookingId: string;
  pnrSearchableRef: string;
  status: string;
  paymentMethod: string;
  totalSellPrice: { amount: string; currency: string };
  createdAt: string;
  voucher: VoucherView;
}

/** SCR-17 (doc/ADREN_UIUX_SPEC.md §12.2) — the Booking Confirmation screen's content. */
export function useBookingConfirmation(bookingId: string | undefined) {
  return useQuery({
    queryKey: ["booking", bookingId],
    queryFn: async () => {
      const { data } = await apiClient.get<BookingView>(`/bookings/${bookingId}`);
      return data;
    },
    enabled: Boolean(bookingId),
  });
}

/** Downloads the real (mock-phase-stubbed) voucher PDF via a Blob, per SCR-17's Download Voucher button. */
export function useDownloadVoucher() {
  return useMutation({
    mutationFn: async (bookingId: string) => {
      const { data } = await apiClient.get(`/bookings/${bookingId}/voucher`, { responseType: "blob" });
      const url = window.URL.createObjectURL(data as Blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `voucher-${bookingId}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    },
  });
}
