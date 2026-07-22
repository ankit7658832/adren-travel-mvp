import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, afterEach, vi } from "vitest";
import { NavBar } from "./NavBar";
import { AuthSessionProvider } from "@/shared/auth/AuthSessionContext";
import { AUTH_TOKEN_STORAGE_KEY } from "@/shared/auth/authTypes";
import { makeFakeToken } from "@/shared/auth/testAuthTokens";

function renderNavBar() {
  return render(
    <AuthSessionProvider>
      <MemoryRouter>
        <NavBar />
      </MemoryRouter>
    </AuthSessionProvider>
  );
}

const originalLocation = window.location;

describe("NavBar (HRD-14 follow-up)", () => {
  afterEach(() => {
    localStorage.clear();
    Object.defineProperty(window, "location", { configurable: true, value: originalLocation });
  });

  it("with no session, shows only Search and Sign in", () => {
    renderNavBar();

    expect(screen.getByRole("link", { name: "Search" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Sign in" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Dashboard" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /sign out/i })).not.toBeInTheDocument();
  });

  it("a CONSULTANT session shows the Consultant links and a Sign out button, not Sign in", () => {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, makeFakeToken({ role: "CONSULTANT" }));
    renderNavBar();

    expect(screen.getByRole("link", { name: "Dashboard" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Users" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Wallet" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Admin Console" })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: /sign out/i })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Sign in" })).not.toBeInTheDocument();
  });

  it("a SUPER_ADMIN session shows the Super Admin links", () => {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, makeFakeToken({ role: "SUPER_ADMIN" }));
    renderNavBar();

    expect(screen.getByRole("link", { name: "Admin Console" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Onboard Consultant" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Dashboard" })).not.toBeInTheDocument();
  });

  it("a USER session shows only its narrower link set", () => {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, makeFakeToken({ role: "USER" }));
    renderNavBar();

    expect(screen.getByRole("link", { name: "PNR Search" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Dashboard" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Admin Console" })).not.toBeInTheDocument();
  });

  it("signing out clears the token and hard-navigates to /login", () => {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, makeFakeToken({ role: "CONSULTANT" }));
    const hrefSetter = vi.fn();
    let currentHref = window.location.href;
    Object.defineProperty(window, "location", {
      configurable: true,
      value: {
        ...window.location,
        get href() {
          return currentHref;
        },
        set href(value: string) {
          currentHref = value;
          hrefSetter(value);
        },
      },
    });

    renderNavBar();
    fireEvent.click(screen.getByRole("button", { name: /sign out/i }));

    expect(localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)).toBeNull();
    expect(hrefSetter).toHaveBeenCalledWith("/login");
  });
});
