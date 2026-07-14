/**
 * doc/architecture/RULES.md §7.1 explicitly designates the white-label
 * theme/branding context as Zustand-owned cross-cutting client state (it's
 * not server data — React Query doesn't fit — and it outlives a single
 * component subtree, so plain useState doesn't either). This is that
 * store: whichever Layer 2 screen is currently mounted (storefront,
 * quotation, voucher) sets the active resolved theme here on mount.
 *
 * Doesn't need a React Context/Provider — that's the point of Zustand
 * over Context for this case, per the same RULES.md section: any
 * component can read it directly via the hook, no provider wiring
 * required. See src/shared/providers/AppProviders.tsx (FES-02) for where
 * providers that *do* need wrapping (e.g. future auth) are slotted.
 */
import { create } from "zustand";
import type { ResolvedTenantTheme } from "./resolveTenantTheme";

interface TenantThemeStore {
  activeTheme: ResolvedTenantTheme | null;
  setActiveTheme: (theme: ResolvedTenantTheme | null) => void;
}

export const useTenantThemeStore = create<TenantThemeStore>((set) => ({
  activeTheme: null,
  setActiveTheme: (theme) => set({ activeTheme: theme }),
}));
