export type Role = "SUPER_ADMIN" | "CONSULTANT" | "USER";

export interface AuthPrincipal {
  userId: string;
  role: Role;
  consultantId: string | null;
}

export const AUTH_TOKEN_STORAGE_KEY = "adren_auth_token";
