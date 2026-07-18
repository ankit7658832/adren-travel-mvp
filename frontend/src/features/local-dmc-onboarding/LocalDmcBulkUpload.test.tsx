import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { LocalDmcBulkUpload } from "./LocalDmcBulkUpload";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { post: vi.fn() },
}));

const LOCAL_DMC_ID = "11111111-1111-1111-1111-111111111111";

function renderWithProviders() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[`/local-dmc/${LOCAL_DMC_ID}/inventory`]}>
        <Routes>
          <Route path="/local-dmc/:id/inventory" element={<LocalDmcBulkUpload />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

function selectCsvFile(content: string, name = "inventory.csv") {
  const file = new File([content], name, { type: "text/csv" });
  const input = screen.getByLabelText(/csv file/i) as HTMLInputElement;
  fireEvent.change(input, { target: { files: [file] } });
}

describe("LocalDmcBulkUpload", () => {
  beforeEach(() => {
    vi.mocked(apiClient.post).mockReset();
  });

  it("default state: the Upload button is disabled until a file is chosen", () => {
    renderWithProviders();

    expect(screen.getByRole("button", { name: /upload/i })).toBeDisabled();
  });

  it("enables Upload once a CSV file is chosen and shows its name", async () => {
    renderWithProviders();

    selectCsvFile("productName,category\nCity Tour,ACTIVITY\n");

    // Reading the file (FileReader.onload) is async — wait on the button's
    // enabled state itself, not just the synchronously-set filename text.
    await waitFor(() => expect(screen.getByRole("button", { name: /upload/i })).toBeEnabled());
    expect(screen.getByText(/inventory\.csv/i)).toBeInTheDocument();
  });

  it("success state: shows the imported count when every row is valid", async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: { successCount: 2, errors: [] } });
    renderWithProviders();
    selectCsvFile("productName,category\nCity Tour,ACTIVITY\nTransfer,TRANSFER\n");
    await waitFor(() => expect(screen.getByRole("button", { name: /upload/i })).toBeEnabled());

    fireEvent.click(screen.getByRole("button", { name: /upload/i }));

    await waitFor(() => {
      expect(screen.getByText(/imported 2 inventory items/i)).toBeInTheDocument();
    });
    expect(apiClient.post).toHaveBeenCalledWith(
      `/local-dmc/${LOCAL_DMC_ID}/inventory/bulk-upload`,
      { csvContent: "productName,category\nCity Tour,ACTIVITY\nTransfer,TRANSFER\n" }
    );
  });

  it("row-error state: shows every row-level, field-level error and imports nothing", async () => {
    vi.mocked(apiClient.post).mockResolvedValue({
      data: {
        successCount: 0,
        errors: [
          { rowNumber: 1, fieldErrors: ["netRate is required", "category is not a recognized value: BOGUS"] },
        ],
      },
    });
    renderWithProviders();
    selectCsvFile("productName,category\nBad Row,BOGUS\n");
    await waitFor(() => expect(screen.getByRole("button", { name: /upload/i })).toBeEnabled());

    fireEvent.click(screen.getByRole("button", { name: /upload/i }));

    await waitFor(() => {
      expect(screen.getByText(/nothing was imported/i)).toBeInTheDocument();
    });
    expect(screen.getByLabelText("csv-row-errors").children).toHaveLength(1);
    expect(screen.getByText(/row 1:/i)).toHaveTextContent("netRate is required; category is not a recognized value: BOGUS");
  });

  it("error state: shows an alert when the upload request itself fails", async () => {
    vi.mocked(apiClient.post).mockRejectedValue(new Error("network error"));
    renderWithProviders();
    selectCsvFile("productName,category\nCity Tour,ACTIVITY\n");
    await waitFor(() => expect(screen.getByRole("button", { name: /upload/i })).toBeEnabled());

    fireEvent.click(screen.getByRole("button", { name: /upload/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not upload this inventory file/i);
    });
  });
});
