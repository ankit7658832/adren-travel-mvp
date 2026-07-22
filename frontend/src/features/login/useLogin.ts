import { useMutation } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";
import { AUTH_TOKEN_STORAGE_KEY, type Role } from "@/shared/auth/authTypes";

export interface LoginInput {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  role: Role;
  consultantId: string | null;
}

/**
 * HRD-14 — the real, credential-checked login (`POST /api/v1/auth/login`),
 * replacing the local-dev-only `DevAuthController` shortcut everywhere
 * outside local development. Stores the returned bearer token under the
 * same `AUTH_TOKEN_STORAGE_KEY` `apiClient`'s request interceptor and
 * `AuthSessionProvider` already read from.
 */
export function useLogin() {
  return useMutation({
    mutationFn: async (input: LoginInput) => {
      const { data } = await apiClient.post<LoginResponse>("/auth/login", input);
      window.localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, data.token);
      return data;
    },
  });
}

/** Where each role lands immediately after a successful login. */
export function landingRouteFor(role: Role): string {
  switch (role) {
    case "SUPER_ADMIN":
      return "/admin";
    case "CONSULTANT":
      return "/dashboard";
    case "USER":
      return "/search";
  }
}
