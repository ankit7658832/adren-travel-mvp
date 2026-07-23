import { useMutation } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

/** SCR-00b — always resolves (200) regardless of whether the email matches an account; never reveals which. */
export function useForgotPassword() {
  return useMutation({
    mutationFn: async (email: string) => {
      await apiClient.post("/auth/forgot-password", { email });
    },
  });
}

export interface ResetPasswordInput {
  token: string;
  newPassword: string;
}

/** SCR-00b — consumes the one-time token from the emailed reset link. */
export function useResetPassword() {
  return useMutation({
    mutationFn: async (input: ResetPasswordInput) => {
      await apiClient.post("/auth/reset-password", input);
    },
  });
}
