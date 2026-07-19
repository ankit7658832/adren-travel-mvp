import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Badge } from "@/shared/design-system/Badge";
import { Button } from "@/shared/design-system/Button";
import { Select } from "@/shared/design-system/Select";
import { TextField } from "@/shared/design-system/TextField";
import { BYOS_SUPPLIER_IDS, useByosCredentialEntry, type ByosSupplierId } from "./useByosCredentialEntry";

/**
 * FES-08 — reference migration proving the react-hook-form + zod pattern
 * ahead of BOK-11/BOK-13/ADS-03/FES-09 all needing it, in place of the
 * bespoke per-field `useState` this screen used before.
 */
const byosCredentialFormSchema = z.object({
  supplierId: z.enum(BYOS_SUPPLIER_IDS),
  secretValue: z.string().min(1, "Credential value is required"),
});

type ByosCredentialFormValues = z.infer<typeof byosCredentialFormSchema>;

/**
 * PRD §10.4, DMC-06 — a Consultant enters their own supplier API
 * credentials (BYOS) so that supplier's inventory is scoped to their
 * account only. The secret value is write-only from this screen: the
 * backend never returns it, only a masked configured/not-configured
 * summary (FND-12's row-level encryption). All 5 PRD Part 21 states.
 */
export function ByosCredentialEntry() {
  const { listQuery, save } = useByosCredentialEntry();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ByosCredentialFormValues>({
    resolver: zodResolver(byosCredentialFormSchema),
    defaultValues: { supplierId: "HOTELBEDS", secretValue: "" },
  });

  function onSubmit(values: ByosCredentialFormValues) {
    save.mutate(values, { onSuccess: () => reset({ supplierId: values.supplierId, secretValue: "" }) });
  }

  const summaryFor = (supplierId: ByosSupplierId) => listQuery.data?.find((c) => c.supplierId === supplierId);

  return (
    <main className="mx-auto max-w-3xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">Your Supplier Credentials (BYOS)</h1>
      <p className="mt-1 text-sm text-neutral-600">
        Connect your own supplier accounts — inventory from these credentials is available only to you.
      </p>

      <form onSubmit={handleSubmit(onSubmit)} className="mt-6 flex flex-col gap-2 sm:flex-row sm:items-end sm:gap-3">
        <Select
          label="Supplier"
          options={BYOS_SUPPLIER_IDS.map((id) => ({ value: id, label: id }))}
          error={errors.supplierId?.message}
          {...register("supplierId")}
        />
        <div className="flex-1">
          <TextField
            label="Your credential value"
            type="password"
            error={errors.secretValue?.message}
            {...register("secretValue")}
          />
        </div>
        <Button type="submit" disabled={save.isPending}>
          {save.isPending ? "Saving…" : "Save"}
        </Button>
      </form>
      {save.isError && (
        <p role="alert" className="mt-2 text-sm text-error-700">
          Could not save this credential. Please try again.
        </p>
      )}
      {save.isSuccess && (
        <p role="status" className="mt-2 text-sm text-success-700">
          Credential saved.
        </p>
      )}

      <div className="mt-8">
        {listQuery.isLoading && (
          <p role="status" className="text-sm text-neutral-600">
            Loading your supplier credentials…
          </p>
        )}

        {listQuery.isError && (
          <div
            role="alert"
            className="flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3"
          >
            <p className="text-sm text-error-700">Could not load your supplier credentials.</p>
            <Button variant="secondary" size="sm" onClick={() => listQuery.refetch()}>
              Retry
            </Button>
          </div>
        )}

        {listQuery.isSuccess && (
          <ul aria-label="byos-credential-list" className="space-y-3">
            {BYOS_SUPPLIER_IDS.map((id) => {
              const summary = summaryFor(id);
              return (
                <li
                  key={id}
                  className="flex items-center justify-between rounded-md border border-neutral-200 bg-surface px-4 py-3"
                >
                  <span className="text-base text-neutral-900">{id}</span>
                  {summary?.configured ? (
                    <Badge tone="success">Configured — •••• masked</Badge>
                  ) : (
                    <Badge tone="neutral">Not configured</Badge>
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </main>
  );
}
