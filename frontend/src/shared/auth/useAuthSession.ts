import { useContext } from "react";
import { AuthSessionContext } from "./authSessionContextInstance";
import type { AuthPrincipal } from "./authTypes";

export function useAuthSession(): AuthPrincipal | null {
  return useContext(AuthSessionContext);
}
