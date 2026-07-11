import { describe, expect, it } from "vitest";
import { renderHook, act, waitFor } from "@testing-library/react";
import { useMultiLocationSearch } from "./useMultiLocationSearch";

describe("useMultiLocationSearch", () => {
  it("starts in idle state", () => {
    const { result } = renderHook(() => useMultiLocationSearch());
    expect(result.current.status).toBe("idle");
    expect(result.current.results).toEqual([]);
  });

  it("transitions idle -> loading -> success and returns one result per location", async () => {
    const { result } = renderHook(() => useMultiLocationSearch());

    act(() => {
      result.current.search(["Goa", "Udaipur", "Jaipur"]);
    });

    await waitFor(() => expect(result.current.status).toBe("success"));
    expect(result.current.results).toHaveLength(3);
  });

  it("does nothing when given an empty location list", async () => {
    const { result } = renderHook(() => useMultiLocationSearch());

    act(() => {
      result.current.search([]);
    });

    expect(result.current.status).toBe("idle");
  });
});
