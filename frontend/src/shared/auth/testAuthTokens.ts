import type { AuthPrincipal } from "./authTypes";

/**
 * Test-only: mints a fake, unsigned-but-correctly-shaped JWT so tests can
 * seed `localStorage` the same way a real login flow eventually will —
 * signature verification is never done client-side (see
 * AuthSessionContext.tsx), so an unsigned token decodes identically to a
 * real one for this layer's purposes.
 */
export function makeFakeToken(principal: Partial<AuthPrincipal> & { role: string }): string {
  const payload = {
    sub: principal.userId ?? "11111111-1111-1111-1111-111111111111",
    role: principal.role,
    consultantId: principal.consultantId ?? undefined,
  };
  const base64 = btoa(JSON.stringify(payload));
  return `header.${base64}.signature`;
}
