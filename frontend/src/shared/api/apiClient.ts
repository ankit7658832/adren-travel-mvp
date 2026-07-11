import axios from "axios";

/**
 * Single axios instance for all backend calls. The Vite dev server proxies
 * /api to the backend (see vite.config.ts), so no base URL is needed here
 * in dev; set VITE_API_BASE_URL for other environments.
 */
export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "/api/v1",
  timeout: 15_000,
});
