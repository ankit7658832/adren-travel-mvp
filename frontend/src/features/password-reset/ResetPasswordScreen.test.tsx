import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { http, HttpResponse } from "msw";
import { ResetPasswordScreen } from "./ResetPasswordScreen";
import { server } from "@/test/mswServer";

function renderScreen(initialEntry = "/reset-password?token=abc-123") {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <ResetPasswordScreen />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe("ResetPasswordScreen (SCR-00b)", () => {
  it("shows a missing-token message when no ?token= is present", () => {
    renderScreen("/reset-password");

    expect(screen.getByRole("alert")).toHaveTextContent(/missing its token/i);
  });

  it("rejects a mismatched confirm password before ever calling the API", () => {
    renderScreen();

    fireEvent.change(screen.getByLabelText(/^new password$/i), { target: { value: "NewPassword1!" } });
    fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: "Different1!" } });
    fireEvent.click(screen.getByRole("button", { name: /reset password/i }));

    expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument();
  });

  it("submits the token and new password, showing a success confirmation", async () => {
    let capturedBody: unknown;
    server.use(
      http.post("/api/v1/auth/reset-password", async ({ request }) => {
        capturedBody = await request.json();
        return new HttpResponse(null, { status: 200 });
      })
    );
    renderScreen();

    fireEvent.change(screen.getByLabelText(/^new password$/i), { target: { value: "NewPassword1!" } });
    fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: "NewPassword1!" } });
    fireEvent.click(screen.getByRole("button", { name: /reset password/i }));

    await waitFor(() => {
      expect(screen.getByRole("status")).toHaveTextContent(/password has been reset/i);
    });
    expect(capturedBody).toEqual({ token: "abc-123", newPassword: "NewPassword1!" });
  });

  it("shows an invalid-link message on a 400 from the backend", async () => {
    server.use(http.post("/api/v1/auth/reset-password", () => HttpResponse.json({ title: "Invalid" }, { status: 400 })));
    renderScreen();

    fireEvent.change(screen.getByLabelText(/^new password$/i), { target: { value: "NewPassword1!" } });
    fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: "NewPassword1!" } });
    fireEvent.click(screen.getByRole("button", { name: /reset password/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/invalid or has expired/i);
    });
  });
});
