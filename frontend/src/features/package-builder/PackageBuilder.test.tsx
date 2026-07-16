import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { PackageBuilder } from "./PackageBuilder";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { post: vi.fn() },
}));

function renderWithQuotationId(quotationId = "quotation-1") {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[`/packages/new?quotationId=${quotationId}`]}>
        <PackageBuilder />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

function fillRequiredFields() {
  fireEvent.change(screen.getByLabelText(/package name/i), { target: { value: "Goa Getaway" } });
  fireEvent.change(screen.getByLabelText(/valid from/i), { target: { value: "2026-08-01" } });
  fireEvent.change(screen.getByLabelText(/valid until/i), { target: { value: "2026-09-01" } });
  fireEvent.change(screen.getByLabelText(/markup price/i), { target: { value: "500" } });
  fireEvent.change(screen.getByLabelText(/max travelers/i), { target: { value: "4" } });
}

describe("PackageBuilder", () => {
  beforeEach(() => {
    vi.mocked(apiClient.post).mockReset();
  });

  it("shows the empty state when no quotation is selected", () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <MemoryRouter initialEntries={["/packages/new"]}>
          <PackageBuilder />
        </MemoryRouter>
      </QueryClientProvider>
    );

    expect(screen.getByText(/no quotation selected/i)).toBeInTheDocument();
  });

  it("blocks submission and shows a validation error when required fields are missing", () => {
    renderWithQuotationId();

    fireEvent.click(screen.getByRole("button", { name: /continue/i }));

    expect(screen.getByRole("alert")).toHaveTextContent(/required fields must be filled in/i);
    expect(apiClient.post).not.toHaveBeenCalled();
  });

  it("creates the package then publishes it on the happy path", async () => {
    vi.mocked(apiClient.post).mockImplementation((url: string) => {
      if (url.endsWith("/package")) {
        return Promise.resolve({ data: { packageId: "package-1" } });
      }
      if (url.endsWith("/publish")) {
        return Promise.resolve({ data: {} });
      }
      return Promise.reject(new Error(`unexpected call: ${url}`));
    });
    renderWithQuotationId();
    fillRequiredFields();

    fireEvent.click(screen.getByRole("button", { name: /continue/i }));
    await waitFor(() => expect(screen.getByRole("button", { name: /^publish$/i })).toBeInTheDocument());
    expect(apiClient.post).toHaveBeenCalledWith("/quotations/quotation-1/package", expect.any(Object));

    fireEvent.click(screen.getByRole("button", { name: /^publish$/i }));

    await waitFor(() => expect(screen.getByText(/package published/i)).toBeInTheDocument());
  });

  it("shows the ATOL disclosure step when publish reports it's required, then publishes after confirming", async () => {
    vi.mocked(apiClient.post).mockImplementation((url: string) => {
      if (url.endsWith("/package")) {
        return Promise.resolve({ data: { packageId: "package-1" } });
      }
      if (url.endsWith("/atol-disclosure")) {
        return Promise.resolve({ data: {} });
      }
      if (url.endsWith("/publish")) {
        // First publish attempt fails with the ATOL-required problem type;
        // any subsequent attempt (after disclosure) succeeds.
        if (vi.mocked(apiClient.post).mock.calls.filter((c) => (c[0] as string).endsWith("/publish")).length === 1) {
          return Promise.reject({
            response: { data: { type: "https://docs.adren.travel/errors/atol-disclosure-required" } },
          });
        }
        return Promise.resolve({ data: {} });
      }
      return Promise.reject(new Error(`unexpected call: ${url}`));
    });
    renderWithQuotationId();
    fillRequiredFields();
    fireEvent.click(screen.getByRole("button", { name: /continue/i }));
    await waitFor(() => screen.getByRole("button", { name: /^publish$/i }));

    fireEvent.click(screen.getByRole("button", { name: /^publish$/i }));

    await waitFor(() => expect(screen.getByText(/atol disclosure required/i)).toBeInTheDocument());
    fireEvent.click(screen.getByRole("button", { name: /confirm atol disclosure/i }));
    await waitFor(() => expect(apiClient.post).toHaveBeenCalledWith("/packages/package-1/atol-disclosure"));

    fireEvent.click(screen.getByRole("button", { name: /^publish$/i }));

    await waitFor(() => expect(screen.getByText(/package published/i)).toBeInTheDocument());
  });
});
