import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export interface ConsultantUserView {
  userId: string;
  email: string;
  displayName: string;
  canCreatePackage: boolean;
}

interface UsersPageResponse {
  content: ConsultantUserView[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

const USERS_QUERY_KEY = ["consultant-users"];

/** PRD §3.3/§21.6 — a Consultant's own Users (FND-09). Server data, so React Query. */
export function useUserManagement() {
  return useQuery({
    queryKey: USERS_QUERY_KEY,
    queryFn: async () => {
      const { data } = await apiClient.get<UsersPageResponse>("/users");
      return data;
    },
  });
}

export function useAddUser() {
  const queryClient = useQueryClient();
  return useMutation({
    // AUTH-01 — password is the new User's real login credential.
    mutationFn: async (input: { email: string; displayName: string; password: string }) => {
      const { data } = await apiClient.post<{ userId: string }>("/users", input);
      return data;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: USERS_QUERY_KEY }),
  });
}

export function useSetUserCapability() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: { userId: string; granted: boolean }) => {
      await apiClient.patch(`/users/${input.userId}/capabilities/CREATE_PACKAGE`, { granted: input.granted });
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: USERS_QUERY_KEY }),
  });
}
