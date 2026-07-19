/**
 * FES-02 — the documented slot for app-wide *context providers*: between
 * QueryClientProvider and BrowserRouter in main.tsx, unless a provider
 * specifically needs router context.
 *
 * Theme/branding state (doc/DESIGN.md §3, §13) is a Zustand store
 * (src/shared/theming/tenantThemeStore.ts) per doc/architecture/RULES.md
 * §7.1's state-management boundary — Zustand stores don't need a React
 * Context/Provider, any component reads them directly via the hook, so
 * branding doesn't occupy this slot. FES-07's AuthSessionProvider is the
 * first real occupant, exactly as predicted here. Nest new providers
 * inside this component rather than each feature choosing its own place
 * to mount one.
 */
import type { ReactNode } from "react";
import { AuthSessionProvider } from "@/shared/auth/AuthSessionContext";

export function AppProviders({ children }: { children: ReactNode }) {
  return <AuthSessionProvider>{children}</AuthSessionProvider>;
}
