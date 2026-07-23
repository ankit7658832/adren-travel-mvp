import { useState } from "react";
import { Link } from "react-router-dom";
import { Button } from "@/shared/design-system/Button";
import { Badge } from "@/shared/design-system/Badge";
import { PreviewBanner } from "./PreviewBanner";
import { PRODUCT_CATEGORIES, type ProductCategoryId } from "./productPreviewData";

type SortOrder = "price-asc" | "price-desc";

/**
 * SCR-04–SCR-08 (doc/ADREN_UIUX_SPEC.md §7) — shared list-page layout
 * across all 5 product categories, per that section's own "described
 * once" instruction. "Add to Itinerary" is disabled here (not a fake
 * working button) — there's no real line item behind any of this
 * category's mock rows for it to actually add.
 */
export function ProductListPage({ categoryId }: { categoryId: ProductCategoryId }) {
  const category = PRODUCT_CATEGORIES[categoryId];
  const [sortOrder, setSortOrder] = useState<SortOrder>("price-asc");

  const sortedItems = [...category.listItems].sort((a, b) =>
    sortOrder === "price-asc" ? a.price - b.price : b.price - a.price
  );

  return (
    <main className="mx-auto max-w-4xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">{category.label}</h1>
      <div className="mt-3">
        <PreviewBanner />
      </div>

      <div className="mt-4 flex items-center justify-end gap-2">
        <label htmlFor="sort-order" className="text-sm text-neutral-700">
          Sort by
        </label>
        <select
          id="sort-order"
          value={sortOrder}
          onChange={(e) => setSortOrder(e.target.value as SortOrder)}
          className="h-9 rounded-md border border-neutral-300 bg-surface px-2 text-sm text-neutral-900"
        >
          <option value="price-asc">Price: low to high</option>
          <option value="price-desc">Price: high to low</option>
        </select>
      </div>

      <ul aria-label={`${category.label.toLowerCase()}-list`} className="mt-4 space-y-3">
        {sortedItems.map((item) => (
          <li key={item.id} className="rounded-md border border-neutral-200 bg-surface p-4">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-base font-medium text-neutral-900">{item.name}</p>
                <div className="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-sm text-neutral-600">
                  {item.fields.map((field) => (
                    <span key={field.label}>
                      {field.label}: {field.value}
                    </span>
                  ))}
                </div>
                <div className="mt-2">
                  <Badge tone="neutral">{item.supplierBadge}</Badge>
                </div>
              </div>
              <div className="flex shrink-0 flex-col items-end gap-2">
                <p className="text-lg font-semibold text-neutral-900">
                  {item.currency} {item.price.toLocaleString()}
                </p>
                <Link to={`/preview/${category.routeSegment}/${item.id}`}>
                  <Button variant="secondary" size="sm">
                    View Details
                  </Button>
                </Link>
                <Button size="sm" disabled title="Preview data — no real itinerary to add to">
                  Add to Itinerary
                </Button>
              </div>
            </div>
          </li>
        ))}
      </ul>
    </main>
  );
}
