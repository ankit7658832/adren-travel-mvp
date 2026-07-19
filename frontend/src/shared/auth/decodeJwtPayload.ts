/**
 * Decodes (never verifies) a JWT's payload segment — client-side JWT
 * decoding is advisory only, never a security boundary; see
 * AuthSessionContext.tsx's Javadoc-equivalent comment for why signature
 * verification client-side would be pointless (the backend already does
 * the real check on every request, per RULES.md §5.1).
 */
export interface JwtPayload {
  sub: string;
  role: string;
  consultantId?: string;
  exp?: number;
}

export function decodeJwtPayload(token: string): JwtPayload | null {
  try {
    const [, payloadSegment] = token.split(".");
    if (!payloadSegment) return null;
    const base64 = payloadSegment.replace(/-/g, "+").replace(/_/g, "/");
    const json = atob(base64);
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}
