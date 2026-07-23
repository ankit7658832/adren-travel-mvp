import { useMutation } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export interface TravelerEntry {
  key: string;
  name: string;
  dateOfBirth: string;
  nationality: string;
  requiresDocuments: boolean;
  passportNumber: string;
  passportExpiry: string;
}

export function emptyTraveler(key: string): TravelerEntry {
  return {
    key,
    name: "",
    dateOfBirth: "",
    nationality: "",
    requiresDocuments: false,
    passportNumber: "",
    passportExpiry: "",
  };
}

export function isTravelerComplete(traveler: TravelerEntry): boolean {
  if (!traveler.name || !traveler.dateOfBirth) {
    return false;
  }
  if (traveler.requiresDocuments && (!traveler.passportNumber || !traveler.passportExpiry)) {
    return false;
  }
  return true;
}

/** PRD §20.10 (BOK-14, SCR-14) — saves one traveler profile via the already-real POST /api/v1/travelers. */
export function useCreateTravelerProfile() {
  return useMutation({
    mutationFn: async (traveler: TravelerEntry) => {
      const { data } = await apiClient.post<{ travelerId: string }>("/travelers", {
        name: traveler.name,
        dateOfBirth: traveler.dateOfBirth,
        passportNumber: traveler.requiresDocuments ? traveler.passportNumber : undefined,
        passportExpiry: traveler.requiresDocuments ? traveler.passportExpiry : undefined,
        nationality: traveler.nationality || undefined,
        documentVaultReferences: [],
        preferences: {},
      });
      return data;
    },
  });
}
