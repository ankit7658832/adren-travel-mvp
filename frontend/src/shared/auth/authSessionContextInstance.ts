import { createContext } from "react";
import type { AuthPrincipal } from "./authTypes";

export const AuthSessionContext = createContext<AuthPrincipal | null>(null);
