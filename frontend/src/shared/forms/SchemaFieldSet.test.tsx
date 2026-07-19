import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { SchemaFieldSet } from "./SchemaFieldSet";
import type { SchemaField } from "./useMarketDependentFields";

const fields: SchemaField[] = [
  { fieldKey: "gstRegistration", label: "GST Registration", required: true },
  { fieldKey: "businessPan", label: "Business PAN", required: false },
];

describe("SchemaFieldSet (FES-09)", () => {
  it("renders nothing for an empty field set", () => {
    const { container } = render(
      <SchemaFieldSet legend="KYC" fields={[]} values={{}} onChange={vi.fn()} />
    );
    expect(container).toBeEmptyDOMElement();
  });

  it("renders one FES-04 TextField per field, with a required marker only on required fields", () => {
    render(<SchemaFieldSet legend="KYC details" fields={fields} values={{}} onChange={vi.fn()} />);

    expect(screen.getByLabelText(/GST Registration \*/)).toBeInTheDocument();
    expect(screen.getByLabelText("Business PAN")).toBeInTheDocument();
    expect(screen.getByText("KYC details")).toBeInTheDocument();
  });

  it("calls onChange with the field key and new value", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<SchemaFieldSet legend="KYC" fields={fields} values={{}} onChange={onChange} />);

    await user.type(screen.getByLabelText("Business PAN"), "X");

    expect(onChange).toHaveBeenCalledWith("businessPan", "X");
  });

  it("surfaces a per-field error via FES-04's TextField aria wiring", () => {
    render(
      <SchemaFieldSet
        legend="KYC"
        fields={fields}
        values={{}}
        onChange={vi.fn()}
        errors={{ gstRegistration: "GST Registration is required" }}
      />
    );

    expect(screen.getByRole("alert")).toHaveTextContent("GST Registration is required");
  });
});
