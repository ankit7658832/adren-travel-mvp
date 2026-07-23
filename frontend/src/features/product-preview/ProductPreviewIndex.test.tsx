import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { ProductPreviewIndex } from "./ProductPreviewIndex";
import { PRODUCT_CATEGORIES } from "./productPreviewData";

describe("ProductPreviewIndex", () => {
  it("shows the preview banner and links to every category's list page", () => {
    render(
      <MemoryRouter>
        <ProductPreviewIndex />
      </MemoryRouter>
    );

    expect(screen.getByRole("note")).toHaveTextContent(/preview — mock data/i);
    for (const category of Object.values(PRODUCT_CATEGORIES)) {
      const link = screen.getByRole("link", { name: new RegExp(category.label) });
      expect(link).toHaveAttribute("href", `/preview/${category.routeSegment}`);
    }
  });
});
