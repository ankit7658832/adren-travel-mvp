import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export interface PackageDetailsInput {
  name: string;
  description: string;
  validityStart: string; // ISO date (yyyy-MM-dd)
  validityEnd: string; // ISO date (yyyy-MM-dd)
  markupPrice: string; // decimal string, matches BigDecimal wire format
  maxPax: string; // integer string
}

interface CreatePackageResponseDto {
  packageId: string;
}

/**
 * PRD §21.3's three-step flow: create the DRAFT package from a Quotation,
 * then publish it — except a UK dynamic flight+hotel combo needs the ATOL
 * disclosure step completed first (BOK-11, PRD §17.2/§22.3 T5). Whether
 * that step is required isn't knowable up front from the frontend's own
 * data — it only surfaces when {@code publish} fails with the backend's
 * 409 ATOL-disclosure-required response (`BookingControllerAdvice`'s
 * `atol-disclosure-required` problem type), so `atolDisclosureRequired`
 * flips reactively off that specific failure rather than being computed
 * client-side.
 */
export function usePackageBuilder(quotationId: string) {
  const [packageId, setPackageId] = useState<string | null>(null);
  const [atolDisclosureRequired, setAtolDisclosureRequired] = useState(false);
  const [atolDisclosureCompleted, setAtolDisclosureCompleted] = useState(false);

  const createPackage = useMutation({
    mutationFn: async (details: PackageDetailsInput) => {
      const { data } = await apiClient.post<CreatePackageResponseDto>(
        `/quotations/${quotationId}/package`,
        {
          name: details.name,
          description: details.description || null,
          validityStart: details.validityStart,
          validityEnd: details.validityEnd,
          markupPrice: details.markupPrice,
          maxPax: Number(details.maxPax),
        },
      );
      return data.packageId;
    },
    onSuccess: (newPackageId) => setPackageId(newPackageId),
  });

  const completeAtolDisclosure = useMutation({
    mutationFn: async () => {
      if (!packageId) throw new Error("No package to complete ATOL disclosure for");
      await apiClient.post(`/packages/${packageId}/atol-disclosure`);
    },
    onSuccess: () => setAtolDisclosureCompleted(true),
  });

  const publish = useMutation({
    mutationFn: async (promoteViaAds: boolean) => {
      if (!packageId) throw new Error("No package to publish");
      await apiClient.post(`/packages/${packageId}/publish`, { promoteViaAds });
    },
    onError: (error) => {
      // RFC 7807 problem type set by BookingControllerAdvice's
      // handleAtolDisclosureRequired — see that class for the exact string.
      const type = isAxiosProblemDetail(error) ? error.response?.data?.type : undefined;
      if (typeof type === "string" && type.endsWith("/atol-disclosure-required")) {
        setAtolDisclosureRequired(true);
      }
    },
  });

  return {
    packageId,
    atolDisclosureRequired,
    atolDisclosureCompleted,
    createPackage,
    completeAtolDisclosure,
    publish,
  };
}

function isAxiosProblemDetail(
  error: unknown,
): error is { response?: { data?: { type?: string } } } {
  return typeof error === "object" && error !== null && "response" in error;
}
