import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi, beforeEach } from "vitest";
import type { ReactNode } from "react";
import { useMarketDependentFields } from "./useMarketDependentFields";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn() },
}));

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe("useMarketDependentFields (FES-09 field-resolution engine)", () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
  });

  it("does not fetch until a market is provided", () => {
    renderHook(() => useMarketDependentFields("/consultants/kyc-rules", null), { wrapper });

    expect(apiClient.get).not.toHaveBeenCalled();
  });

  it("fetches the given endpoint with the market as a query param", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: [{ fieldKey: "abn", label: "ABN", required: true }] });

    const { result } = renderHook(() => useMarketDependentFields("/consultants/kyc-rules", "AUSTRALIA"), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(apiClient.get).toHaveBeenCalledWith("/consultants/kyc-rules", { params: { market: "AUSTRALIA" } });
    expect(result.current.data).toEqual([{ fieldKey: "abn", label: "ABN", required: true }]);
  });

  it("FES-09: the field set changes when the backend rule table (mocked response) changes for a different market", async () => {
    vi.mocked(apiClient.get).mockImplementation(async (_url, config) => {
      const market = (config as { params: { market: string } }).params.market;
      return market === "INDIA"
        ? { data: [{ fieldKey: "gstRegistration", label: "GST Registration", required: true }] }
        : { data: [{ fieldKey: "companiesHouseNumber", label: "Companies House Number", required: true }] };
    });

    const { result, rerender } = renderHook(
      ({ market }: { market: string }) => useMarketDependentFields("/consultants/kyc-rules", market),
      { wrapper, initialProps: { market: "INDIA" } }
    );
    await waitFor(() => expect(result.current.data?.[0]?.fieldKey).toBe("gstRegistration"));

    rerender({ market: "UK" });
    await waitFor(() => expect(result.current.data?.[0]?.fieldKey).toBe("companiesHouseNumber"));
  });
});
