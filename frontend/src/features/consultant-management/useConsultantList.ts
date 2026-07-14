import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export type ConsultantStatus = "ACTIVE" | "SUSPENDED";

export interface ConsultantView {
  consultantId: string;
  businessName: string;
  homeMarket: string;
  status: ConsultantStatus;
  createdAt: string;
}

interface ConsultantsPageResponse {
  content: ConsultantView[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

const CONSULTANTS_QUERY_KEY = ["consultants"];

/** PRD §3.1/§21.6 — Super Admin Console's Consultants list (FND-05). Server data, so React Query. */
export function useConsultantList() {
  return useQuery({
    queryKey: CONSULTANTS_QUERY_KEY,
    queryFn: async () => {
      const { data } = await apiClient.get<ConsultantsPageResponse>("/consultants");
      return data;
    },
  });
}

export function useUpdateConsultantStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: { consultantId: string; status: ConsultantStatus }) => {
      await apiClient.patch(`/consultants/${input.consultantId}/status`, { status: input.status });
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CONSULTANTS_QUERY_KEY }),
  });
}
