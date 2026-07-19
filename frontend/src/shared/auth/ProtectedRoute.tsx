import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useAuthSession } from "./useAuthSession";
import type { Role } from "./authTypes";

export interface ProtectedRouteProps {
  allowedRoles: Role[];
  children: ReactNode;
}

/**
 * FES-07's own acceptance criterion: wraps a protected `<Route>`'s
 * element and redirects *before* the wrapped screen ever mounts when the
 * current session's role isn't in `allowedRoles` — no flash of
 * unauthorized content. Redirects to `/` (Search Dashboard), which every
 * role — including no session at all — is allowed to reach per PRD §6, so
 * this can never itself create a redirect loop.
 */
export function ProtectedRoute({ allowedRoles, children }: ProtectedRouteProps) {
  const principal = useAuthSession();

  if (!principal || !allowedRoles.includes(principal.role)) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}
