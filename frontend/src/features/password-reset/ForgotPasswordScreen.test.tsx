import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { http, HttpResponse } from "msw";
import { ForgotPasswordScreen } from "./ForgotPasswordScreen";
import { server } from "@/test/mswServer";

function renderScreen() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ForgotPasswordScreen />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe("ForgotPasswordScreen (SCR-00b)", () => {
  it("shows the check-your-email confirmation on submit, regardless of whether the email exists", async () => {
    let capturedEmail: unknown;
    server.use(
      http.post("/api/v1/auth/forgot-password", async ({ request }) => {
        capturedEmail = (await request.json() as { email: string }).email;
        return new HttpResponse(null, { status: 200 });
      })
    );
    renderScreen();

    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: "owner@testco.example" } });
    fireEvent.click(screen.getByRole("button", { name: /send reset link/i }));

    await waitFor(() => {
      expect(screen.getByRole("status")).toHaveTextContent(/check your email/i);
    });
    expect(capturedEmail).toBe("owner@testco.example");
  });

  it("shows an error state when the request itself fails", async () => {
    server.use(http.post("/api/v1/auth/forgot-password", () => HttpResponse.json({ title: "Server error" }, { status: 500 })));
    renderScreen();

    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: "owner@testco.example" } });
    fireEvent.click(screen.getByRole("button", { name: /send reset link/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not send the reset link/i);
    });
  });
});
