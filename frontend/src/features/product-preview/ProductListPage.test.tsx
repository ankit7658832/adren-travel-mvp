import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { ProductListPage } from "./ProductListPage";
import { PRODUCT_CATEGORIES } from "./productPreviewData";

function renderList(categoryId: keyof typeof PRODUCT_CATEGORIES) {
  return render(
    <MemoryRouter>
      <ProductListPage categoryId={categoryId} />
    </MemoryRouter>
  );
}

describe("ProductListPage (SCR-04–08)", () => {
  it("shows the preview banner so mock data is never mistaken for live inventory", () => {
    renderList("hotel");

    expect(screen.getByRole("note")).toHaveTextContent(/preview — mock data/i);
  });

  it("renders one row per item for the given category, with a disabled Add to Itinerary button", () => {
    renderList("hotel");

    const list = screen.getByLabelText("hotels-list");
    expect(list.children).toHaveLength(PRODUCT_CATEGORIES.hotel.listItems.length);
    expect(screen.getAllByRole("button", { name: /add to itinerary/i })[0]).toBeDisabled();
  });

  it("renders a different item set for a different category", () => {
    renderList("flight");

    expect(screen.getByRole("heading", { name: "Flights" })).toBeInTheDocument();
    expect(screen.getByLabelText("flights-list").children).toHaveLength(
      PRODUCT_CATEGORIES.flight.listItems.length
    );
  });

  it("sorting by price changes the row order", () => {
    renderList("hotel");
    const list = screen.getByLabelText("hotels-list");
    const cheaperFirstOrder = Array.from(list.children).map((li) => li.textContent);

    fireEvent.change(screen.getByLabelText(/sort by/i), { target: { value: "price-desc" } });

    const pricierFirstOrder = Array.from(list.children).map((li) => li.textContent);
    expect(pricierFirstOrder).not.toEqual(cheaperFirstOrder);
  });

  it("links each row's View Details button to the product's preview detail route", () => {
    renderList("hotel");

    const firstItem = PRODUCT_CATEGORIES.hotel.listItems[0];
    const link = screen.getAllByRole("link", { name: /view details/i })[0];
    expect(link).toHaveAttribute("href", `/preview/hotels/${firstItem.id}`);
  });
});
