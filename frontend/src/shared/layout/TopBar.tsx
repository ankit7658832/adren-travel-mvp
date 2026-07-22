import { Link } from "react-router-dom";
import { LogIn, LogOut } from "lucide-react";
import { useAuthSession } from "@/shared/auth/useAuthSession";
import { AUTH_TOKEN_STORAGE_KEY } from "@/shared/auth/authTypes";
import { useTenantBranding } from "@/shared/theming/useTenantBranding";

/**
 * doc/ADREN_UIUX_SPEC.md §3 — Consultant/User Shell: tenant white-label
 * logo (Layer 2) plus a small fixed "Powered by Adren" wordmark. Super
 * Admin Shell: fixed Adren branding only, no tenant theming (doc/DESIGN.md
 * §10 row 21.6 — "the console chrome itself is pure Adren"), so the tenant
 * logo fetch below only ever runs for CONSULTANT/USER.
 */
export function TopBar() {
  const principal = useAuthSession();
  const showsTenantLogo = principal?.role === "CONSULTANT" || principal?.role === "USER";
  const brandingQuery = useTenantBranding(showsTenantLogo ? (principal?.consultantId ?? "") : "");
  const tenantLogoUrl = brandingQuery.isSuccess ? brandingQuery.data.logoUrl : null;

  function handleSignOut() {
    window.localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
    window.location.href = "/login";
  }

  return (
    <header className="flex h-14 shrink-0 items-center justify-between border-b border-neutral-200 bg-surface px-4">
      <div className="flex items-center gap-2">
        {tenantLogoUrl ? (
          <>
            <img src={tenantLogoUrl} alt="Consultant logo" className="h-7 w-auto" />
            <span className="text-xs text-neutral-500">Powered by Adren</span>
          </>
        ) : (
          <span className="text-sm font-semibold text-neutral-900">Adren</span>
        )}
      </div>

      {principal ? (
        <button
          type="button"
          onClick={handleSignOut}
          className="flex items-center gap-1.5 text-sm font-medium text-neutral-700 hover:text-primary-600"
        >
          <LogOut aria-hidden="true" className="h-4 w-4" />
          Sign out
        </button>
      ) : (
        <Link to="/login" className="flex items-center gap-1.5 text-sm font-medium text-primary-600 hover:underline">
          <LogIn aria-hidden="true" className="h-4 w-4" />
          Sign in
        </Link>
      )}
    </header>
  );
}
