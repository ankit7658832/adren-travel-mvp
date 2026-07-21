import axios from "axios";
import { AUTH_TOKEN_STORAGE_KEY } from "@/shared/auth/authTypes";

/**
 * Single axios instance for all backend calls. The Vite dev server proxies
 * /api to the backend (see vite.config.ts), so no base URL is needed here
 * in dev; set VITE_API_BASE_URL for other environments.
 */
export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "/api/v1",
  timeout: 15_000,
});

// TST-03 — every real endpoint except a tiny allowlist requires auth
// (backend SecurityConfig: .anyRequest().authenticated()), but nothing
// attached the token AuthSessionContext already reads from localStorage
// to any outgoing request — every real API call 401'd. Discovered while
// building a Playwright e2e spec that finally logs in for real (via the
// dev-only token-minting endpoint) instead of navigating straight past
// auth like every prior spec's own documented gap.
apiClient.interceptors.request.use((config) => {
  const token = window.localStorage.getItem(AUTH_TOKEN_STORAGE_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
