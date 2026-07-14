import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { UserManagement } from "./UserManagement";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn(), post: vi.fn(), patch: vi.fn() },
}));

function renderWithQueryClient() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <UserManagement />
    </QueryClientProvider>
  );
}

describe("UserManagement", () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
    vi.mocked(apiClient.post).mockReset();
    vi.mocked(apiClient.patch).mockReset();
  });

  it("loading state: shows a loading message while fetching users", () => {
    vi.mocked(apiClient.get).mockReturnValue(new Promise(() => {}));
    renderWithQueryClient();

    expect(screen.getByRole("status")).toHaveTextContent(/loading users/i);
  });

  it("empty state: shows a message when there are no users yet", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 } });
    renderWithQueryClient();

    await waitFor(() => {
      expect(screen.getByText(/no users yet/i)).toBeInTheDocument();
    });
  });

  it("success state: renders one row per user, with a badge for granted capability", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: {
        content: [
          { userId: "u1", email: "a@b.com", displayName: "Alice", canCreatePackage: true },
          { userId: "u2", email: "c@d.com", displayName: "Charlie", canCreatePackage: false },
        ],
        page: 0,
        size: 20,
        totalElements: 2,
        totalPages: 1,
      },
    });
    renderWithQueryClient();

    await waitFor(() => {
      expect(screen.getByLabelText("user-list").children).toHaveLength(2);
    });
    expect(screen.getByText("Can create packages")).toBeInTheDocument();
  });

  it("error state: shows a retry option when the users fetch fails", async () => {
    vi.mocked(apiClient.get).mockRejectedValue(new Error("network error"));
    renderWithQueryClient();

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not load users/i);
    });
    expect(screen.getByRole("button", { name: /retry/i })).toBeInTheDocument();
  });

  it("adds a user via the form", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 } });
    vi.mocked(apiClient.post).mockResolvedValue({ data: { userId: "new-user" } });
    renderWithQueryClient();

    fireEvent.change(screen.getByLabelText(/^email$/i), { target: { value: "staff@example.com" } });
    fireEvent.change(screen.getByLabelText(/display name/i), { target: { value: "Staff Member" } });
    fireEvent.click(screen.getByRole("button", { name: /add user/i }));

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith("/users", { email: "staff@example.com", displayName: "Staff Member" });
    });
  });
});
