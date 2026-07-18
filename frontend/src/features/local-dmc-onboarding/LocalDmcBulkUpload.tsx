import { useState, type ChangeEvent } from "react";
import { useParams } from "react-router-dom";
import { Button } from "@/shared/design-system/Button";
import { useLocalDmcBulkUpload } from "./useLocalDmcBulkUpload";

/**
 * PRD §10.2.8, DMC-03 — bulk-upload a Local DMC's inventory catalogue from
 * a CSV file rather than entering products one at a time. All 5 PRD Part
 * 21 states: default (no file chosen), loading, success (every row
 * imported), empty is N/A here (a CSV with zero data rows just imports
 * zero items, not a distinct error), and error (row-level, field-level
 * validation failures — nothing was imported).
 */
export function LocalDmcBulkUpload() {
  const { id: localDmcId } = useParams<{ id: string }>();
  const upload = useLocalDmcBulkUpload(localDmcId ?? "");
  const [fileName, setFileName] = useState<string | null>(null);
  const [csvContent, setCsvContent] = useState<string | null>(null);

  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }
    setFileName(file.name);
    const reader = new FileReader();
    reader.onload = () => setCsvContent(typeof reader.result === "string" ? reader.result : null);
    reader.readAsText(file);
  }

  function handleUpload() {
    if (csvContent) {
      upload.mutate(csvContent);
    }
  }

  return (
    <section className="mx-auto max-w-3xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">Bulk-upload Local DMC inventory</h1>
      <p className="mt-1 text-sm text-neutral-600">
        CSV columns: productName, category, netRate, netRateCurrency, cancellationPolicyText, availableFrom,
        availableTo.
      </p>

      <div className="mt-6 rounded-md border border-neutral-200 bg-surface p-4">
        <label htmlFor="dmc-inventory-csv" className="mb-1 block text-sm font-medium text-neutral-700">
          CSV file
        </label>
        <input id="dmc-inventory-csv" type="file" accept=".csv,text/csv" onChange={handleFileChange} />
        {fileName && <p className="mt-2 text-sm text-neutral-600">Selected: {fileName}</p>}

        <Button className="mt-4" onClick={handleUpload} disabled={!csvContent || upload.isPending}>
          Upload
        </Button>
      </div>

      {upload.isPending && (
        <p role="status" className="mt-4 text-sm text-neutral-600">
          Uploading inventory…
        </p>
      )}

      {upload.isError && (
        <p role="alert" className="mt-4 text-sm text-error-700">
          Could not upload this inventory file.
        </p>
      )}

      {upload.isSuccess && upload.data.errors.length === 0 && (
        <p role="status" className="mt-4 text-sm text-success-700">
          Imported {upload.data.successCount} inventory item{upload.data.successCount === 1 ? "" : "s"}.
        </p>
      )}

      {upload.isSuccess && upload.data.errors.length > 0 && (
        <div role="alert" className="mt-4 rounded-md border border-error-600/20 bg-error-50 px-4 py-3">
          <p className="text-sm text-error-700">
            Nothing was imported — fix the following rows and upload again.
          </p>
          <ul aria-label="csv-row-errors" className="mt-2 space-y-1">
            {upload.data.errors.map((rowError) => (
              <li key={rowError.rowNumber} className="text-sm text-error-700">
                Row {rowError.rowNumber}: {rowError.fieldErrors.join("; ")}
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}
