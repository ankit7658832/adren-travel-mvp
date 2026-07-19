import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { useSearchParams } from "react-router-dom";
import { z } from "zod";
import { Button } from "@/shared/design-system/Button";
import { Select } from "@/shared/design-system/Select";
import { TextField } from "@/shared/design-system/TextField";
import { usePublishedPackages, useCreateCampaign, useSubmitCampaignInputs } from "./useCampaignBuilder";
import { CreativeVariantGallery } from "./CreativeVariantGallery";

const campaignInputsSchema = z.object({
  packageId: z.string().min(1, "Select a package to promote"),
  audienceDescription: z.string().min(1, "Audience description is required"),
  budgetCapAmount: z.coerce.number({ message: "Budget is required" }).positive("Budget must be greater than 0"),
  durationDays: z.coerce.number({ message: "Duration is required" }).int().positive("Duration must be at least 1 day"),
});

// z.coerce.number() means the form's raw INPUT type (string, pre-coercion)
// differs from its OUTPUT type (number, post-coercion) — useForm needs both
// generics or TypeScript infers the input type for the resolver mismatch.
type CampaignInputsFormInput = z.input<typeof campaignInputsSchema>;
type CampaignInputsFormOutput = z.output<typeof campaignInputsSchema>;

/**
 * PRD §14.2 steps 1-2, §21.8 — the Campaign Builder: select a published
 * Package (pre-populated from `?packageId=` when arriving via Package
 * Builder's "Promote this Package" opt-in, ADS-03's own AC) and provide
 * audience/budget/duration, all required before the campaign can proceed
 * to creative generation (ADS-04).
 *
 * States implemented: loading (fetching published packages), error (fetch
 * failed), empty (no published packages to promote yet), success (the
 * form, then the post-submit confirmation) — all 4 PRD Part 21 states;
 * there is no meaningfully distinct "default" state once packages load,
 * since the form itself is what a Consultant sees immediately.
 */
export function CampaignBuilder() {
  const [searchParams] = useSearchParams();
  const prefilledPackageId = searchParams.get("packageId") ?? "";

  const packagesQuery = usePublishedPackages();
  const createCampaign = useCreateCampaign();
  const submitInputs = useSubmitCampaignInputs();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CampaignInputsFormInput, unknown, CampaignInputsFormOutput>({
    resolver: zodResolver(campaignInputsSchema),
    defaultValues: { packageId: prefilledPackageId, audienceDescription: "", budgetCapAmount: undefined, durationDays: undefined },
  });

  async function onSubmit(values: CampaignInputsFormOutput) {
    const campaign = await createCampaign.mutateAsync(values.packageId);
    await submitInputs.mutateAsync({
      campaignId: campaign.campaignId,
      audienceDescription: values.audienceDescription,
      budgetCapAmount: values.budgetCapAmount,
      durationDays: values.durationDays,
    });
  }

  if (packagesQuery.isLoading) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-8">
        <h1 className="text-2xl font-semibold text-neutral-900">21.8 Campaign Builder</h1>
        <p role="status" className="mt-4 text-sm text-neutral-600">
          Loading your published packages…
        </p>
      </main>
    );
  }

  if (packagesQuery.isError) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-8">
        <h1 className="text-2xl font-semibold text-neutral-900">21.8 Campaign Builder</h1>
        <div role="alert" className="mt-4 flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3">
          <p className="text-sm text-error-700">Could not load your published packages.</p>
          <Button variant="secondary" size="sm" onClick={() => packagesQuery.refetch()}>
            Retry
          </Button>
        </div>
      </main>
    );
  }

  const packages = packagesQuery.data ?? [];

  if (packages.length === 0) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-8">
        <h1 className="text-2xl font-semibold text-neutral-900">21.8 Campaign Builder</h1>
        <p className="mt-4 text-sm text-neutral-600">
          You have no published packages yet — publish a package from the Package Builder first, then opt into
          &quot;Promote this Package&quot; to start a campaign.
        </p>
      </main>
    );
  }

  if (submitInputs.isSuccess) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-8">
        <h1 className="text-2xl font-semibold text-neutral-900">Campaign started</h1>
        <p role="status" className="mt-4 text-sm text-neutral-600">
          Your campaign is now pending approval — generate ad creative next.
        </p>
        <CreativeVariantGallery campaignId={submitInputs.data.campaignId} />
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-2xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">21.8 Campaign Builder</h1>

      <form onSubmit={handleSubmit(onSubmit)} className="mt-6 flex flex-col gap-4">
        <Select
          label="Package"
          options={packages.map((p) => ({ value: p.packageId, label: p.name }))}
          error={errors.packageId?.message}
          {...register("packageId")}
        />
        <TextField
          label="Audience description"
          error={errors.audienceDescription?.message}
          {...register("audienceDescription")}
        />
        <div className="grid grid-cols-2 gap-4">
          <TextField
            label="Budget cap"
            inputMode="decimal"
            error={errors.budgetCapAmount?.message}
            {...register("budgetCapAmount")}
          />
          <TextField
            label="Duration (days)"
            inputMode="numeric"
            error={errors.durationDays?.message}
            {...register("durationDays")}
          />
        </div>

        {(createCampaign.isError || submitInputs.isError) && (
          <p role="alert" className="text-sm text-error-700">
            Could not start this campaign. Please try again.
          </p>
        )}

        <div className="flex justify-end">
          <Button type="submit" disabled={createCampaign.isPending || submitInputs.isPending}>
            {createCampaign.isPending || submitInputs.isPending ? "Starting…" : "Continue"}
          </Button>
        </div>
      </form>
    </main>
  );
}
