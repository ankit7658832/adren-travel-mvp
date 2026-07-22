import { useMutation, useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export interface PackageDetails {
  packageId: string;
  sourceItineraryId: string;
  consultantId: string;
  name: string;
  description: string;
  validityStart: string;
  validityEnd: string;
  basePrice: string; // decimal string, matches BigDecimal wire format
  markupPrice: string;
  currency: string;
  maxPax: number;
  promotedViaAds: boolean;
}

/** HRD-15/BOK-13 — the Package a User is booking, for the price-breakdown step. */
export function usePackageDetails(packageId: string | undefined) {
  return useQuery({
    queryKey: ["package", packageId],
    queryFn: async () => {
      const { data } = await apiClient.get<PackageDetails>(`/packages/${packageId}`);
      return data;
    },
    enabled: Boolean(packageId),
  });
}

export interface TravelerDetailsInput {
  name: string;
  dateOfBirth: string;
  passportNumber?: string;
  passportExpiry?: string;
  nationality?: string;
}

/** PRD §20.10 (BOK-14) — captures the traveler profile before booking confirmation. */
export function useCreateTravelerProfile() {
  return useMutation({
    mutationFn: async (input: TravelerDetailsInput) => {
      const { data } = await apiClient.post<{ travelerId: string }>("/travelers", {
        ...input,
        documentVaultReferences: [],
        preferences: {},
      });
      return data;
    },
  });
}

export interface ConfirmBookingInput {
  quotationOrPackageId: string;
  totalSellPrice: string;
  currency: string;
  paymentMethod: "WALLET" | "ON_ACCOUNT";
}

/** PRD §9.1 Flow C, §21.4 (BOK-13) — confirms the booking via wallet hold or On-Account billing. */
export function useConfirmBooking() {
  return useMutation({
    mutationFn: async (input: ConfirmBookingInput) => {
      const path = input.paymentMethod === "ON_ACCOUNT" ? "/bookings/on-account" : "/bookings";
      const { data } = await apiClient.post<{ bookingId: string }>(path, {
        quotationOrPackageId: input.quotationOrPackageId,
        totalSellPrice: input.totalSellPrice,
        currency: input.currency,
      });
      return data;
    },
  });
}
