import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, beforeEach, afterEach, vi } from "vitest";
import { http, HttpResponse } from "msw";
import { LoginScreen } from "./LoginScreen";
import { server } from "@/test/mswServer";

const originalLocation = window.location;

function renderLoginScreen() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <LoginScreen />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe("LoginScreen (HRD-14)", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    Object.defineProperty(window, "location", { configurable: true, value: originalLocation });
  });

  it("submits email/password and redirects to the role's landing route on success", async () => {
    server.use(
      http.post("/api/v1/auth/login", async ({ request }) => {
        const body = (await request.json()) as { email: string; password: string };
        expect(body).toEqual({ email: "owner@testco.example", password: "InitialPassword1!" });
        return HttpResponse.json({ token: "fake-jwt", role: "CONSULTANT", consultantId: "c-1" });
      })
    );
    // jsdom doesn't implement real navigation (window.location.pathname
    // never actually changes) — stub just the href setter so the redirect
    // target is observable, while keeping href readable (axios resolves
    // its relative baseURL against window.location.href internally).
    const hrefSetter = vi.fn();
    let currentHref = window.location.href;
    Object.defineProperty(window, "location", {
      configurable: true,
      value: {
        ...window.location,
        get href() { return currentHref; },
        set href(value: string) { currentHref = value; hrefSetter(value); },
      },
    });

    renderLoginScreen();
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: "owner@testco.example" } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: "InitialPassword1!" } });
    fireEvent.click(screen.getByRole("button", { name: /sign in/i }));

    await waitFor(() => {
      expect(hrefSetter).toHaveBeenCalledWith("/dashboard");
    });
    expect(localStorage.getItem("adren_auth_token")).toBe("fake-jwt");
  });

  it("shows a clear invalid-credentials error on a 401, without redirecting", async () => {
    server.use(
      http.post("/api/v1/auth/login", () =>
        HttpResponse.json({ title: "Invalid credentials", detail: "Invalid email or password." }, { status: 401 })
      )
    );
    renderLoginScreen();

    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: "wrong@testco.example" } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: "WrongPassword1!" } });
    fireEvent.click(screen.getByRole("button", { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/invalid email or password/i);
    });
  });

  it("shows a generic error on an unexpected server failure", async () => {
    server.use(http.post("/api/v1/auth/login", () => HttpResponse.json({ title: "Server error" }, { status: 500 })));
    renderLoginScreen();

    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: "owner@testco.example" } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: "InitialPassword1!" } });
    fireEvent.click(screen.getByRole("button", { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not sign in/i);
    });
  });
});
