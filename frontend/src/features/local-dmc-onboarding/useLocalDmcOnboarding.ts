import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export interface LocalDmcView {
  localDmcId: string;
  consultantId: string;
  businessName: string;
  productCategories: string[];
  sampleRatesSummary: string | null;
  referencesInfo: string | null;
  status: "PENDING" | "ACTIVE";
  verificationNotes: string | null;
  cancellationRate: number;
  complaintCount: number;
  flagged: boolean;
  inventoryStale: boolean;
  createdAt: string;
}

interface LocalDmcPageResponse {
  content: LocalDmcView[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SubmitLocalDmcInput {
  businessName: string;
  productCategories: string[];
  sampleRatesSummary: string;
  referencesInfo: string;
}

const LOCAL_DMC_QUERY_KEY = ["local-dmc"];

/**
 * PRD §10.3/§20.14, DMC-01/02/05 — submit a new Local DMC for onboarding,
 * and browse the caller's own (Consultant) or every Consultant's (Super
 * Admin, when the backend recognizes the role) records. Server data, so
 * React Query. The backend resolves "my own" from the JWT — this hook
 * never needs to know its own consultantId (no login/session story has
 * landed yet).
 */
export function useLocalDmcOnboarding() {
  const queryClient = useQueryClient();

  const listQuery = useQuery({
    queryKey: LOCAL_DMC_QUERY_KEY,
    queryFn: async () => {
      const { data } = await apiClient.get<LocalDmcPageResponse>("/local-dmc", { params: { size: 50 } });
      return data;
    },
  });

  const submit = useMutation({
    mutationFn: async (input: SubmitLocalDmcInput) => {
      const { data } = await apiClient.post<{ localDmcId: string }>("/local-dmc", input);
      return data.localDmcId;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: LOCAL_DMC_QUERY_KEY }),
  });

  const activate = useMutation({
    mutationFn: async (params: { localDmcId: string; verificationNotes: string }) => {
      await apiClient.post(`/local-dmc/${params.localDmcId}/activate`, { verificationNotes: params.verificationNotes });
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: LOCAL_DMC_QUERY_KEY }),
  });

  return { listQuery, submit, activate };
}
