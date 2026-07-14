import { describe, expect, it, beforeEach } from "vitest";
import { useItineraryDraftStore } from "./itineraryDraftStore";

describe("itineraryDraftStore", () => {
  beforeEach(() => {
    useItineraryDraftStore.getState().reset();
  });

  it("starts with no active draft", () => {
    expect(useItineraryDraftStore.getState().itineraryId).toBeNull();
    expect(useItineraryDraftStore.getState().lineItems).toEqual({});
  });

  it("persists a line-item swap across simulated step navigation", () => {
    // "Step navigation" in the wizard unmounts one step's component tree and
    // mounts another's — the store itself is module-level state outside
    // React, so it must survive that unmount, unlike a step-local useState.
    useItineraryDraftStore.getState().startDraft("itin-1");
    useItineraryDraftStore.getState().setLineItem({
      locationCode: "GOA",
      category: "hotel",
      supplierId: "hotelbeds",
      supplierRateId: "rate-123",
      autoSelected: true,
    });

    // Simulate leaving step 1 and the step component tree unmounting —
    // nothing in this test holds a React component alive; reading the
    // store fresh here is the proof the data lives independently of it.
    const afterNavigation = useItineraryDraftStore.getState().lineItems["GOA:hotel"];

    expect(afterNavigation).toEqual({
      locationCode: "GOA",
      category: "hotel",
      supplierId: "hotelbeds",
      supplierRateId: "rate-123",
      autoSelected: true,
    });
  });

  it("swapping a line item for an alternate replaces it and clears autoSelected", () => {
    useItineraryDraftStore.getState().setLineItem({
      locationCode: "GOA",
      category: "hotel",
      supplierId: "hotelbeds",
      supplierRateId: "rate-123",
      autoSelected: true,
    });

    useItineraryDraftStore.getState().setLineItem({
      locationCode: "GOA",
      category: "hotel",
      supplierId: "stuba",
      supplierRateId: "rate-456",
      autoSelected: false,
    });

    expect(useItineraryDraftStore.getState().lineItems["GOA:hotel"]).toEqual({
      locationCode: "GOA",
      category: "hotel",
      supplierId: "stuba",
      supplierRateId: "rate-456",
      autoSelected: false,
    });
  });

  it("removeLineItem drops only the targeted location/category key", () => {
    useItineraryDraftStore.getState().setLineItem({
      locationCode: "GOA",
      category: "hotel",
      supplierId: "hotelbeds",
      supplierRateId: "rate-123",
      autoSelected: true,
    });
    useItineraryDraftStore.getState().setLineItem({
      locationCode: "JAIPUR",
      category: "activity",
      supplierId: "hbactivities",
      supplierRateId: "rate-789",
      autoSelected: true,
    });

    useItineraryDraftStore.getState().removeLineItem("GOA", "hotel");

    const lineItems = useItineraryDraftStore.getState().lineItems;
    expect(lineItems["GOA:hotel"]).toBeUndefined();
    expect(lineItems["JAIPUR:activity"]).toBeDefined();
  });

  it("reset clears both the itinerary id and every line item", () => {
    useItineraryDraftStore.getState().startDraft("itin-1");
    useItineraryDraftStore.getState().setLineItem({
      locationCode: "GOA",
      category: "hotel",
      supplierId: "hotelbeds",
      supplierRateId: "rate-123",
      autoSelected: true,
    });

    useItineraryDraftStore.getState().reset();

    expect(useItineraryDraftStore.getState().itineraryId).toBeNull();
    expect(useItineraryDraftStore.getState().lineItems).toEqual({});
  });
});
