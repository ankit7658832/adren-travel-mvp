import { Badge } from "@/shared/design-system/Badge";
import { Button } from "@/shared/design-system/Button";
import { Card } from "@/shared/design-system/Card";
import { useCreativeVariants, useGenerateCreativeVariants } from "./useCampaignBuilder";

export interface CreativeVariantGalleryProps {
  campaignId: string;
}

const VARIANT_COUNT = 3;

/**
 * PRD §14.2 step 3, §21.8 — the creative-variant gallery: triggers AI-12's
 * generation grounded in the campaign's own Package content/live price,
 * then displays the surviving variants (ADS-04's own AC).
 *
 * States implemented: default (not yet generated — a "Generate" action),
 * loading (generation pending), success (persisted variants re-fetched, or
 * a fresh generation result rendered directly), empty (AI-05's explicit
 * "no viable creative" outcome, not an error), error (the generation call
 * itself failed — network/server, distinct from "no viable creative").
 */
export function CreativeVariantGallery({ campaignId }: CreativeVariantGalleryProps) {
  const persistedQuery = useCreativeVariants(campaignId);
  const generate = useGenerateCreativeVariants();

  if (persistedQuery.isLoading || generate.isPending) {
    return (
      <p role="status" className="mt-4 text-sm text-neutral-600">
        Generating ad creative…
      </p>
    );
  }

  if (persistedQuery.isError) {
    return (
      <div role="alert" className="mt-4 flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3">
        <p className="text-sm text-error-700">Could not load creative variants.</p>
        <Button variant="secondary" size="sm" onClick={() => persistedQuery.refetch()}>
          Retry
        </Button>
      </div>
    );
  }

  if (generate.isError) {
    return (
      <p role="alert" className="mt-4 text-sm text-error-700">
        Could not generate ad creative. Please try again.
      </p>
    );
  }

  if (generate.isSuccess && generate.data.type === "NO_VIABLE_AD_CREATIVE") {
    return (
      <p className="mt-4 text-sm text-neutral-600">
        No AI-generated creative could be verified against this package&apos;s real content. Try again, or write your
        own creative.
      </p>
    );
  }

  const persistedVariants = persistedQuery.data ?? [];
  const freshVariants = generate.isSuccess && generate.data.type === "AD_CREATIVE_SUGGESTION" ? generate.data.variants : [];

  if (persistedVariants.length === 0 && freshVariants.length === 0) {
    return (
      <div className="mt-4">
        <Button onClick={() => generate.mutate({ campaignId, variantCount: VARIANT_COUNT })}>
          Generate ad creative
        </Button>
      </div>
    );
  }

  return (
    <div className="mt-4">
      <h2 className="text-sm font-semibold text-neutral-900">Ad creative variants</h2>
      <ul aria-label="creative-variant-gallery" className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-2">
        {persistedVariants.map((variant) => (
          <li key={variant.variantId}>
            <Card padding="sm">
              <p className="text-base font-medium text-neutral-900">{variant.headline}</p>
              <p className="mt-1 text-sm text-neutral-600">{variant.bodyText}</p>
              {variant.approved && <Badge tone="success">Approved</Badge>}
            </Card>
          </li>
        ))}
        {freshVariants.map((variant, index) => (
          <li key={`fresh-${index}`}>
            <Card padding="sm">
              <p className="text-base font-medium text-neutral-900">{variant.headline}</p>
              <p className="mt-1 text-sm text-neutral-600">{variant.bodyText}</p>
            </Card>
          </li>
        ))}
      </ul>
    </div>
  );
}
