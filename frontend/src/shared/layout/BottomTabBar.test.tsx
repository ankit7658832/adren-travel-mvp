import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, afterEach } from "vitest";
import { BottomTabBar } from "./BottomTabBar";
import { AuthSessionProvider } from "@/shared/auth/AuthSessionContext";
import { AUTH_TOKEN_STORAGE_KEY } from "@/shared/auth/authTypes";
import { makeFakeToken } from "@/shared/auth/testAuthTokens";

function renderBottomTabBar() {
  return render(
    <AuthSessionProvider>
      <MemoryRouter>
        <BottomTabBar />
      </MemoryRouter>
    </AuthSessionProvider>
  );
}

describe("BottomTabBar (doc/DESIGN.md §5, below-md degradation)", () => {
  afterEach(() => {
    localStorage.clear();
  });

  it("with no session, shows only Search", () => {
    renderBottomTabBar();

    expect(screen.getByRole("link", { name: /search/i })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /dashboard/i })).not.toBeInTheDocument();
  });

  it("a CONSULTANT session also shows its primary destination (Dashboard)", () => {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, makeFakeToken({ role: "CONSULTANT" }));
    renderBottomTabBar();

    expect(screen.getByRole("link", { name: /search/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /dashboard/i })).toBeInTheDocument();
  });

  it("a SUPER_ADMIN session shows Admin as its primary destination", () => {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, makeFakeToken({ role: "SUPER_ADMIN" }));
    renderBottomTabBar();

    expect(screen.getByRole("link", { name: /admin/i })).toBeInTheDocument();
  });
});
