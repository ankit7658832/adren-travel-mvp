import { describe, expect, it, vi, beforeEach } from "vitest";
import { renderHook, act, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { useMultiLocationSearch } from "./useMultiLocationSearch";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { post: vi.fn() },
}));

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe("useMultiLocationSearch", () => {
  beforeEach(() => {
    vi.mocked(apiClient.post).mockReset();
  });

  it("starts in idle state", () => {
    const { result } = renderHook(() => useMultiLocationSearch(), { wrapper });
    expect(result.current.status).toBe("idle");
    expect(result.current.results).toEqual([]);
  });

  it("transitions idle -> loading -> success and returns one result per location", async () => {
    vi.mocked(apiClient.post).mockResolvedValue({
      data: {
        locations: ["Goa", "Udaipur", "Jaipur"].map((name) => ({
          locationCode: name,
          displayName: name,
          latitude: 20,
          longitude: 80,
          hasInventory: true,
        })),
      },
    });
    const { result } = renderHook(() => useMultiLocationSearch(), { wrapper });

    act(() => {
      result.current.search(["Goa", "Udaipur", "Jaipur"]);
    });

    await waitFor(() => expect(result.current.status).toBe("success"));
    expect(result.current.results).toHaveLength(3);
    expect(apiClient.post).toHaveBeenCalledWith("/search", { locationQueries: ["Goa", "Udaipur", "Jaipur"] });
  });

  it("does nothing when given an empty location list", async () => {
    const { result } = renderHook(() => useMultiLocationSearch(), { wrapper });

    act(() => {
      result.current.search([]);
    });

    expect(result.current.status).toBe("idle");
    expect(apiClient.post).not.toHaveBeenCalled();
  });

  it("transitions to the error state when the request fails", async () => {
    vi.mocked(apiClient.post).mockRejectedValue(new Error("Network error"));
    const { result } = renderHook(() => useMultiLocationSearch(), { wrapper });

    act(() => {
      result.current.search(["Goa"]);
    });

    await waitFor(() => expect(result.current.status).toBe("error"));
    expect(result.current.errorMessage).toBe("Network error");
  });
});
