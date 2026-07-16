import { useState, type FormEvent, type ReactNode } from "react";
import { useSearchParams } from "react-router-dom";
import { usePackageBuilder, type PackageDetailsInput } from "./usePackageBuilder";
import { Button } from "@/shared/design-system/Button";

const EMPTY_DETAILS: PackageDetailsInput = {
  name: "",
  description: "",
  validityStart: "",
  validityEnd: "",
  markupPrice: "",
  maxPax: "",
};

/**
 * PRD Section 21.3 — Package Builder (BOK-11): a form over a selected
 * Quotation (`?quotationId=`), then a publish step gated by the UK ATOL
 * disclosure requirement (PRD §17.2/§22.3 T5) when the backend reports it's
 * needed — see `usePackageBuilder`'s comment on why that can't be known
 * client-side ahead of the publish attempt.
 *
 * States implemented: empty (no quotationId in the URL), default (form),
 * loading (mutation pending, disables submit), success (package created →
 * publish step, then published confirmation), error (inline, retry-able).
 */
export function PackageBuilder() {
  const [searchParams] = useSearchParams();
  const quotationId = searchParams.get("quotationId");

  if (!quotationId) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-8">
        <h1 className="text-2xl font-semibold text-neutral-900">Package Builder</h1>
        <p className="mt-4 text-sm text-neutral-600">
          No Quotation selected. Convert a Quotation to a Package from the Quotation screen first.
        </p>
      </main>
    );
  }

  return <PackageBuilderForQuotation quotationId={quotationId} />;
}

function PackageBuilderForQuotation({ quotationId }: { quotationId: string }) {
  const [details, setDetails] = useState<PackageDetailsInput>(EMPTY_DETAILS);
  const [promoteViaAds, setPromoteViaAds] = useState(false);
  const [attemptedSubmit, setAttemptedSubmit] = useState(false);
  const {
    packageId, atolDisclosureRequired, atolDisclosureCompleted,
    createPackage, completeAtolDisclosure, publish,
  } = usePackageBuilder(quotationId);

  const missingRequiredFields =
    !details.name || !details.validityStart || !details.validityEnd || !details.markupPrice || !details.maxPax;

  function handleCreate(e: FormEvent) {
    e.preventDefault();
    setAttemptedSubmit(true);
    if (missingRequiredFields) return;
    createPackage.mutate(details);
  }

  function handlePublish() {
    publish.mutate(promoteViaAds);
  }

  if (publish.isSuccess) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-8">
        <h1 className="text-2xl font-semibold text-neutral-900">Package published</h1>
        <p className="mt-4 text-sm text-neutral-600">
          &quot;{details.name}&quot; is now visible to your Users.
        </p>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-2xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">21.3 Package Builder</h1>

      {!packageId && (
        <form onSubmit={handleCreate} className="mt-6 flex flex-col gap-4">
          <Field label="Package name" htmlFor="package-name" required>
            <input
              id="package-name"
              value={details.name}
              onChange={(e) => setDetails({ ...details, name: e.target.value })}
              className={inputClassName}
            />
          </Field>
          <Field label="Description" htmlFor="package-description">
            <textarea
              id="package-description"
              value={details.description}
              onChange={(e) => setDetails({ ...details, description: e.target.value })}
              className={inputClassName}
            />
          </Field>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Valid from" htmlFor="package-validity-start" required>
              <input
                id="package-validity-start"
                type="date"
                value={details.validityStart}
                onChange={(e) => setDetails({ ...details, validityStart: e.target.value })}
                className={inputClassName}
              />
            </Field>
            <Field label="Valid until" htmlFor="package-validity-end" required>
              <input
                id="package-validity-end"
                type="date"
                value={details.validityEnd}
                onChange={(e) => setDetails({ ...details, validityEnd: e.target.value })}
                className={inputClassName}
              />
            </Field>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Markup price" htmlFor="package-markup-price" required>
              <input
                id="package-markup-price"
                inputMode="decimal"
                value={details.markupPrice}
                onChange={(e) => setDetails({ ...details, markupPrice: e.target.value })}
                className={inputClassName}
              />
            </Field>
            <Field label="Max travelers" htmlFor="package-max-pax" required>
              <input
                id="package-max-pax"
                inputMode="numeric"
                value={details.maxPax}
                onChange={(e) => setDetails({ ...details, maxPax: e.target.value })}
                className={inputClassName}
              />
            </Field>
          </div>

          {attemptedSubmit && missingRequiredFields && (
            <p role="alert" className="text-sm text-error-700">
              All required fields must be filled in before you can continue.
            </p>
          )}
          {createPackage.isError && (
            <p role="alert" className="text-sm text-error-700">
              Could not create the package. Please try again.
            </p>
          )}

          <div className="flex justify-end">
            <Button type="submit" disabled={createPackage.isPending}>
              {createPackage.isPending ? "Creating…" : "Continue"}
            </Button>
          </div>
        </form>
      )}

      {packageId && !atolDisclosureRequired && (
        <div className="mt-6 flex flex-col gap-4">
          <div className="rounded-md border border-neutral-200 bg-surface px-4 py-3">
            <p className="text-sm text-neutral-700">Package summary</p>
            <p className="mt-1 text-base font-medium text-neutral-900">{details.name}</p>
          </div>
          <label className="flex items-center gap-2 text-sm text-neutral-700">
            <input
              type="checkbox"
              checked={promoteViaAds}
              onChange={(e) => setPromoteViaAds(e.target.checked)}
            />
            Promote this Package
          </label>
          {publish.isError && !atolDisclosureRequired && (
            <p role="alert" className="text-sm text-error-700">
              Could not publish the package. Please try again.
            </p>
          )}
          <div className="flex justify-end">
            <Button onClick={handlePublish} disabled={publish.isPending}>
              {publish.isPending ? "Publishing…" : "Publish"}
            </Button>
          </div>
        </div>
      )}

      {packageId && atolDisclosureRequired && (
        <div
          role="alert"
          className="mt-6 flex flex-col gap-4 rounded-md border border-warning-500/40 bg-warning-50 px-4 py-4"
        >
          <p className="text-sm font-medium text-neutral-900">ATOL disclosure required</p>
          <p className="text-sm text-neutral-700">
            This Package combines a flight and a hotel for a UK Consultant, which requires ATOL protection
            disclosure (PRD §17.2) before it can be published.
          </p>
          {!atolDisclosureCompleted ? (
            <div className="flex justify-end">
              <Button onClick={() => completeAtolDisclosure.mutate()} disabled={completeAtolDisclosure.isPending}>
                {completeAtolDisclosure.isPending ? "Confirming…" : "Confirm ATOL disclosure"}
              </Button>
            </div>
          ) : (
            <div className="flex justify-end">
              <Button onClick={handlePublish} disabled={publish.isPending}>
                {publish.isPending ? "Publishing…" : "Publish"}
              </Button>
            </div>
          )}
        </div>
      )}
    </main>
  );
}

const inputClassName =
  "h-10 w-full rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900 " +
  "placeholder:text-neutral-500 focus:outline-none focus-visible:ring-2 focus-visible:ring-focus-ring focus-visible:ring-offset-2";

interface FieldProps {
  label: string;
  htmlFor: string;
  required?: boolean;
  children: ReactNode;
}

function Field({ label, htmlFor, required, children }: FieldProps) {
  return (
    <div>
      <label htmlFor={htmlFor} className="mb-1 block text-sm font-medium text-neutral-700">
        {label}
        {required && <span aria-hidden="true"> *</span>}
      </label>
      {children}
    </div>
  );
}
