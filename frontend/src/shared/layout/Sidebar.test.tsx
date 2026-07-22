import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, afterEach } from "vitest";
import { Sidebar } from "./Sidebar";
import { AuthSessionProvider } from "@/shared/auth/AuthSessionContext";
import { AUTH_TOKEN_STORAGE_KEY } from "@/shared/auth/authTypes";
import { makeFakeToken } from "@/shared/auth/testAuthTokens";

function renderSidebar() {
  return render(
    <AuthSessionProvider>
      <MemoryRouter>
        <Sidebar />
      </MemoryRouter>
    </AuthSessionProvider>
  );
}

describe("Sidebar (doc/ADREN_UIUX_SPEC.md §3 Global Navigation Shell)", () => {
  afterEach(() => {
    localStorage.clear();
  });

  it("with no session, shows only the public Search link", () => {
    renderSidebar();

    expect(screen.getByRole("link", { name: "Search" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Dashboard" })).not.toBeInTheDocument();
  });

  it("a CONSULTANT session shows the Consultant Shell's links, not the Super Admin Shell's", () => {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, makeFakeToken({ role: "CONSULTANT" }));
    renderSidebar();

    expect(screen.getByRole("link", { name: "Dashboard" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Users" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Wallet" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Admin Console" })).not.toBeInTheDocument();
  });

  it("a SUPER_ADMIN session shows the Super Admin Shell's links", () => {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, makeFakeToken({ role: "SUPER_ADMIN" }));
    renderSidebar();

    expect(screen.getByRole("link", { name: "Admin Console" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Onboard Consultant" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Dashboard" })).not.toBeInTheDocument();
  });

  it("a USER session shows its narrower link set", () => {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, makeFakeToken({ role: "USER" }));
    renderSidebar();

    expect(screen.getByRole("link", { name: /bookings/i })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Admin Console" })).not.toBeInTheDocument();
  });
});
