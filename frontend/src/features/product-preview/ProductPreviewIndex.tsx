import { Link } from "react-router-dom";
import { Card } from "@/shared/design-system/Card";
import { PreviewBanner } from "./PreviewBanner";
import { PRODUCT_CATEGORIES } from "./productPreviewData";

/**
 * Landing page for SCR-04–13's preview screens — the Sidebar links here
 * (a single "Product Preview" entry) rather than to all 5 categories
 * directly, so the persistent nav doesn't grow by 5 more items for
 * screens that are a design reference, not a real product surface.
 */
export function ProductPreviewIndex() {
  return (
    <main className="mx-auto max-w-2xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">Product Preview</h1>
      <div className="mt-3">
        <PreviewBanner />
      </div>

      <ul className="mt-6 space-y-3">
        {Object.values(PRODUCT_CATEGORIES).map((category) => (
          <li key={category.id}>
            <Link to={`/preview/${category.routeSegment}`}>
              <Card padding="sm" className="hover:bg-neutral-50">
                <p className="text-base font-medium text-neutral-900">{category.label}</p>
                <p className="text-sm text-neutral-600">{category.listItems.length} preview items</p>
              </Card>
            </Link>
          </li>
        ))}
      </ul>
    </main>
  );
}
