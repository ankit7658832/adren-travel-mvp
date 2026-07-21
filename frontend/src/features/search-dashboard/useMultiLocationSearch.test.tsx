import { describe, expect, it } from "vitest";
import { renderHook, act, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { http, HttpResponse } from "msw";
import { useMultiLocationSearch } from "./useMultiLocationSearch";
import { server } from "@/test/mswServer";

// TST-04 — MSW intercepts the real HTTP call apiClient makes (see
// src/test/setup.ts), rather than mocking the apiClient module itself.
// The reference usage: every request-shape assertion below fails if the
// hook ever sends the wrong method/path/body, not just if it "calls
// apiClient.post" with some payload — a real regression a mocked module
// can't catch.
let capturedRequestBody: unknown;

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe("useMultiLocationSearch", () => {
  it("starts in idle state", () => {
    const { result } = renderHook(() => useMultiLocationSearch(), { wrapper });
    expect(result.current.status).toBe("idle");
    expect(result.current.results).toEqual([]);
  });

  it("transitions idle -> loading -> success and returns one result per location", async () => {
    server.use(
      http.post("/api/v1/search", async ({ request }) => {
        capturedRequestBody = await request.json();
        return HttpResponse.json({
          locations: ["Goa", "Udaipur", "Jaipur"].map((name) => ({
            locationCode: name,
            displayName: name,
            latitude: 20,
            longitude: 80,
            hasInventory: true,
          })),
        });
      }),
    );
    const { result } = renderHook(() => useMultiLocationSearch(), { wrapper });

    act(() => {
      result.current.search(["Goa", "Udaipur", "Jaipur"]);
    });

    await waitFor(() => expect(result.current.status).toBe("success"));
    expect(result.current.results).toHaveLength(3);
    expect(capturedRequestBody).toEqual({ locationQueries: ["Goa", "Udaipur", "Jaipur"] });
  });

  it("does nothing when given an empty location list", async () => {
    // No handler registered — setup.ts's onUnhandledRequest: "error" means
    // an incorrect real request here would fail this test loudly, a
    // stronger guarantee than a mocked module's "not.toHaveBeenCalled()".
    const { result } = renderHook(() => useMultiLocationSearch(), { wrapper });

    act(() => {
      result.current.search([]);
    });

    expect(result.current.status).toBe("idle");
  });

  it("transitions to the error state when the request fails", async () => {
    server.use(http.post("/api/v1/search", () => HttpResponse.json({ message: "Network error" }, { status: 500 })));
    const { result } = renderHook(() => useMultiLocationSearch(), { wrapper });

    act(() => {
      result.current.search(["Goa"]);
    });

    await waitFor(() => expect(result.current.status).toBe("error"));
  });
});
