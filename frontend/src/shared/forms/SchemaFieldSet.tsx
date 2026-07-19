import { TextField } from "@/shared/design-system/TextField";
import type { SchemaField } from "./useMarketDependentFields";

export interface SchemaFieldSetProps {
  legend: string;
  fields: SchemaField[];
  values: Record<string, string>;
  onChange: (fieldKey: string, value: string) => void;
  errors?: Record<string, string | undefined>;
}

/**
 * FES-09 — renders whatever field set `useMarketDependentFields` resolved,
 * using FES-04's `TextField` for the label/aria wiring every field gets
 * automatically. Purely data-driven: no per-market conditional lives here
 * or anywhere else frontend-side.
 */
export function SchemaFieldSet({ legend, fields, values, onChange, errors }: SchemaFieldSetProps) {
  if (fields.length === 0) {
    return null;
  }

  return (
    <fieldset className="space-y-3">
      <legend className="text-sm font-medium text-neutral-700">{legend}</legend>
      {fields.map((field) => (
        <TextField
          key={field.fieldKey}
          label={field.required ? `${field.label} *` : field.label}
          required={field.required}
          value={values[field.fieldKey] ?? ""}
          onChange={(e) => onChange(field.fieldKey, e.target.value)}
          error={errors?.[field.fieldKey]}
        />
      ))}
    </fieldset>
  );
}
