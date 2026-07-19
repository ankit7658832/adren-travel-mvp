import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { ConsultantOnboardingWizard } from "./ConsultantOnboardingWizard";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn(), post: vi.fn() },
}));

function renderWizard() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <ConsultantOnboardingWizard />
    </QueryClientProvider>
  );
}

describe("ConsultantOnboardingWizard", () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
    vi.mocked(apiClient.post).mockReset();
  });

  it("default state: no market selected, no KYC fields shown yet", () => {
    renderWizard();

    expect(screen.getByLabelText(/home market/i)).toBeInTheDocument();
    expect(screen.queryByText(/kyc details for/i)).not.toBeInTheDocument();
  });

  it("loading state: shows a loading message while fetching KYC rules for the selected market", async () => {
    vi.mocked(apiClient.get).mockReturnValue(new Promise(() => {})); // never resolves
    renderWizard();

    fireEvent.change(screen.getByLabelText(/home market/i), { target: { value: "INDIA" } });

    expect(screen.getByRole("status")).toHaveTextContent(/loading required fields/i);
  });

  it("success state: renders the fetched KYC fields for the selected market", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: [
        { fieldKey: "gstRegistration", label: "GST Registration", required: true },
        { fieldKey: "businessPan", label: "Business PAN", required: true },
      ],
    });
    renderWizard();

    fireEvent.change(screen.getByLabelText(/home market/i), { target: { value: "INDIA" } });

    await waitFor(() => {
      expect(screen.getByLabelText(/gst registration/i)).toBeInTheDocument();
    });
    expect(screen.getByLabelText(/business pan/i)).toBeInTheDocument();
  });

  it("empty state: shows a message when a market has no configured KYC fields", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: [] });
    renderWizard();

    fireEvent.change(screen.getByLabelText(/home market/i), { target: { value: "INDIA" } });

    await waitFor(() => {
      expect(screen.getByText(/no kyc fields are configured/i)).toBeInTheDocument();
    });
  });

  it("error state: shows a retry-worthy message when the KYC rules fetch fails", async () => {
    vi.mocked(apiClient.get).mockRejectedValue(new Error("network error"));
    renderWizard();

    fireEvent.change(screen.getByLabelText(/home market/i), { target: { value: "INDIA" } });

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not load required fields/i);
    });
  });

  it("FES-09: renders correctly for two different markets, with genuinely different fields fetched per market, not a hardcoded map", async () => {
    vi.mocked(apiClient.get).mockImplementation(async (_url, config) => {
      if (config?.params?.market === "INDIA") {
        return { data: [{ fieldKey: "gstRegistration", label: "GST Registration", required: true }] };
      }
      return { data: [{ fieldKey: "companiesHouseNumber", label: "Companies House Number", required: true }] };
    });
    renderWizard();

    fireEvent.change(screen.getByLabelText(/home market/i), { target: { value: "INDIA" } });
    await waitFor(() => expect(screen.getByLabelText(/gst registration/i)).toBeInTheDocument());
    expect(screen.queryByLabelText(/companies house number/i)).not.toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/home market/i), { target: { value: "UK" } });
    await waitFor(() => expect(screen.getByLabelText(/companies house number/i)).toBeInTheDocument());
    expect(screen.queryByLabelText(/gst registration/i)).not.toBeInTheDocument();
  });

  it("FES-09: the rendered field set reflects whatever the backend rule table returns, not a value baked into the frontend", async () => {
    // First render: the rule table has one field for this market.
    vi.mocked(apiClient.get).mockResolvedValueOnce({
      data: [{ fieldKey: "abnNumber", label: "ABN Number", required: true }],
    });
    const { unmount } = renderWizard();
    fireEvent.change(screen.getByLabelText(/home market/i), { target: { value: "AUSTRALIA" } });
    await waitFor(() => expect(screen.getByLabelText(/abn number/i)).toBeInTheDocument());
    unmount();

    // Simulate the backend rule table changing (an admin adds a second
    // required field for this market) — a fresh mount re-fetches and must
    // reflect the new set, since nothing about this field set is hardcoded
    // frontend-side.
    vi.mocked(apiClient.get).mockResolvedValueOnce({
      data: [
        { fieldKey: "abnNumber", label: "ABN Number", required: true },
        { fieldKey: "gstRegistrationAu", label: "GST Registration (AU)", required: true },
      ],
    });
    renderWizard();
    fireEvent.change(screen.getByLabelText(/home market/i), { target: { value: "AUSTRALIA" } });

    await waitFor(() => expect(screen.getByLabelText(/gst registration \(au\)/i)).toBeInTheDocument());
    expect(screen.getByLabelText(/abn number/i)).toBeInTheDocument();
  });

  it("submits the onboarding request and shows a success confirmation with the new consultant id", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: [{ fieldKey: "gstRegistration", label: "GST Registration", required: true }],
    });
    vi.mocked(apiClient.post).mockResolvedValue({ data: { consultantId: "abc-123" } });
    renderWizard();

    fireEvent.change(screen.getByLabelText(/business name/i), { target: { value: "Test Co" } });
    fireEvent.change(screen.getByLabelText(/home market/i), { target: { value: "INDIA" } });

    await waitFor(() => expect(screen.getByLabelText(/gst registration/i)).toBeInTheDocument());
    fireEvent.change(screen.getByLabelText(/gst registration/i), { target: { value: "GST1" } });
    fireEvent.click(screen.getByRole("button", { name: /onboard consultant/i }));

    await waitFor(() => {
      expect(screen.getByRole("status")).toHaveTextContent(/consultant onboarded — id abc-123/i);
    });
    expect(apiClient.post).toHaveBeenCalledWith("/consultants", {
      businessName: "Test Co",
      homeMarket: "INDIA",
      kycFields: { gstRegistration: "GST1" },
    });
  });
});
