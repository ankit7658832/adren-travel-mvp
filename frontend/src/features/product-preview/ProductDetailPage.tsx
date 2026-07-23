import { useParams, Link } from "react-router-dom";
import { Button } from "@/shared/design-system/Button";
import { Card } from "@/shared/design-system/Card";
import { PreviewBanner } from "./PreviewBanner";
import { PRODUCT_CATEGORIES, type ProductCategoryId } from "./productPreviewData";

/**
 * SCR-09–SCR-13 (doc/ADREN_UIUX_SPEC.md §8) — shared detail-page layout
 * across all 5 product categories, per that section's own "shared layout
 * pattern" instruction. Sticky summary card + sections list, same as
 * every category's own spec row describes, populated from
 * productPreviewData.ts's mock fixtures (see that file's doc comment).
 */
export function ProductDetailPage({ categoryId }: { categoryId: ProductCategoryId }) {
  const category = PRODUCT_CATEGORIES[categoryId];
  const { productId } = useParams<{ productId: string }>();
  const detail = productId ? category.details[productId] : undefined;

  if (!detail) {
    return (
      <main className="mx-auto max-w-3xl px-6 py-8">
        <div role="alert" className="rounded-md border border-error-600/20 bg-error-50 px-4 py-3 text-sm text-error-700">
          No {category.singularLabel.toLowerCase()} found for this preview id.
        </div>
        <Link to={`/preview/${category.routeSegment}`} className="mt-4 inline-block text-sm text-primary-600 hover:underline">
          Back to {category.label}
        </Link>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-3xl px-6 py-8">
      <PreviewBanner />

      <div className="mt-6 flex h-40 items-center justify-center rounded-md bg-neutral-100 text-sm text-neutral-500">
        Image gallery placeholder — no real supplier images in this mock phase
      </div>

      <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          <h1 className="text-2xl font-semibold text-neutral-900">{detail.name}</h1>
          {detail.sections.map((section) => (
            <Card key={section.heading}>
              <h2 className="text-lg font-medium text-neutral-900">{section.heading}</h2>
              <p className="mt-2 text-sm text-neutral-700">{section.body}</p>
            </Card>
          ))}
        </div>

        <Card className="h-fit lg:sticky lg:top-6">
          <p className="text-2xl font-semibold text-neutral-900">
            {detail.currency} {detail.price.toLocaleString()}
          </p>
          <dl className="mt-4 space-y-2 text-sm">
            {detail.keyFacts.map((fact) => (
              <div key={fact.label} className="flex justify-between">
                <dt className="text-neutral-600">{fact.label}</dt>
                <dd className="font-medium text-neutral-900">{fact.value}</dd>
              </div>
            ))}
          </dl>
          <Button disabled title="Preview data — no real itinerary to add to" className="mt-4 w-full">
            Add to Itinerary
          </Button>
        </Card>
      </div>
    </main>
  );
}
