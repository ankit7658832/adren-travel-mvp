import { renderHook } from "@testing-library/react";
import { describe, expect, it, afterEach } from "vitest";
import { AuthSessionProvider } from "./AuthSessionContext";
import { useAuthSession } from "./useAuthSession";
import { AUTH_TOKEN_STORAGE_KEY } from "./authTypes";
import { makeFakeToken } from "./testAuthTokens";

describe("AuthSessionContext (FES-07)", () => {
  afterEach(() => {
    localStorage.clear();
  });

  it("returns null with no token in localStorage", () => {
    const { result } = renderHook(() => useAuthSession(), { wrapper: AuthSessionProvider });

    expect(result.current).toBeNull();
  });

  it("decodes a stored token into a principal mirroring AdrenPrincipal's shape", () => {
    localStorage.setItem(
      AUTH_TOKEN_STORAGE_KEY,
      makeFakeToken({ role: "CONSULTANT", userId: "22222222-2222-2222-2222-222222222222", consultantId: "cons-1" })
    );

    const { result } = renderHook(() => useAuthSession(), { wrapper: AuthSessionProvider });

    expect(result.current).toEqual({
      userId: "22222222-2222-2222-2222-222222222222",
      role: "CONSULTANT",
      consultantId: "cons-1",
    });
  });

  it("returns null for a malformed token rather than throwing", () => {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, "not-a-jwt");

    const { result } = renderHook(() => useAuthSession(), { wrapper: AuthSessionProvider });

    expect(result.current).toBeNull();
  });

  it("SUPER_ADMIN's consultantId is null, mirroring AdrenPrincipal's own invariant", () => {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, makeFakeToken({ role: "SUPER_ADMIN" }));

    const { result } = renderHook(() => useAuthSession(), { wrapper: AuthSessionProvider });

    expect(result.current?.role).toBe("SUPER_ADMIN");
    expect(result.current?.consultantId).toBeNull();
  });
});
