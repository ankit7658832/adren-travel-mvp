import { useMemo, type ReactNode } from "react";
import { decodeJwtPayload } from "./decodeJwtPayload";
import { AuthSessionContext } from "./authSessionContextInstance";
import { AUTH_TOKEN_STORAGE_KEY, type AuthPrincipal, type Role } from "./authTypes";

/**
 * FES-07 — mirrors FND-01's `AdrenPrincipal` (userId/role/consultantId) on
 * the frontend so per-role route guards (`ProtectedRoute`) can redirect
 * before a protected screen ever mounts. This is a UX convenience only,
 * never a security boundary — the JWT's signature is never verified
 * client-side, because every real authorization decision is already
 * enforced by the backend's own `@PreAuthorize` checks on every request
 * (RULES.md §5.1). A tampered/forged token here can at most make a route
 * render that then 401s/403s on its first real API call; it can never
 * grant real access to data.
 *
 * No login/token-issuance endpoint exists yet, on either side —
 * `JwtTokenService.generateToken`'s own Javadoc says so explicitly
 * ("no story requests one yet"). This reads whatever token a future login
 * screen would write to `localStorage` under `AUTH_TOKEN_STORAGE_KEY`; no
 * token present means no session, which every guard treats as
 * "not authorized" rather than a special case.
 */
export function AuthSessionProvider({ children }: { children: ReactNode }) {
  const principal = useMemo<AuthPrincipal | null>(() => {
    const token = localStorage.getItem(AUTH_TOKEN_STORAGE_KEY);
    if (!token) return null;

    const payload = decodeJwtPayload(token);
    if (!payload?.role || !payload.sub) return null;

    return {
      userId: payload.sub,
      role: payload.role as Role,
      consultantId: payload.consultantId ?? null,
    };
  }, []);

  return <AuthSessionContext.Provider value={principal}>{children}</AuthSessionContext.Provider>;
}
