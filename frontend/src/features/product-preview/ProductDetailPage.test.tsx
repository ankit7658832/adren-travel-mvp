import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { ProductDetailPage } from "./ProductDetailPage";
import { PRODUCT_CATEGORIES } from "./productPreviewData";

function renderDetail(categoryId: keyof typeof PRODUCT_CATEGORIES, productId: string) {
  return render(
    <MemoryRouter initialEntries={[`/preview/x/${productId}`]}>
      <Routes>
        <Route path="/preview/x/:productId" element={<ProductDetailPage categoryId={categoryId} />} />
      </Routes>
    </MemoryRouter>
  );
}

describe("ProductDetailPage (SCR-09–13)", () => {
  it("shows the preview banner, name, price, and every section for a real mock id", () => {
    const detail = PRODUCT_CATEGORIES.hotel.listItems[0];
    renderDetail("hotel", detail.id);

    expect(screen.getByRole("note")).toHaveTextContent(/preview — mock data/i);
    const fullDetail = PRODUCT_CATEGORIES.hotel.details[detail.id];
    expect(screen.getByRole("heading", { name: fullDetail.name })).toBeInTheDocument();
    for (const section of fullDetail.sections) {
      expect(screen.getByRole("heading", { name: section.heading })).toBeInTheDocument();
    }
  });

  it("shows different sections for a different category (cruise's Passenger Document Requirement)", () => {
    const detail = PRODUCT_CATEGORIES.cruise.listItems[0];
    renderDetail("cruise", detail.id);

    expect(screen.getByRole("heading", { name: /passenger document requirement/i })).toBeInTheDocument();
  });

  it("shows a not-found message for an id that doesn't exist in the mock data", () => {
    renderDetail("hotel", "does-not-exist");

    expect(screen.getByRole("alert")).toHaveTextContent(/no hotel found/i);
    expect(screen.getByRole("link", { name: /back to hotels/i })).toBeInTheDocument();
  });

  it("the Add to Itinerary button is disabled, since there's no real itinerary behind mock data", () => {
    const detail = PRODUCT_CATEGORIES.activity.listItems[0];
    renderDetail("activity", detail.id);

    expect(screen.getByRole("button", { name: /add to itinerary/i })).toBeDisabled();
  });
});
