import { render, screen } from "@testing-library/react";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { describe, expect, it, afterEach } from "vitest";
import { AuthSessionProvider } from "./AuthSessionContext";
import { AUTH_TOKEN_STORAGE_KEY } from "./authTypes";
import { ProtectedRoute } from "./ProtectedRoute";
import { makeFakeToken } from "./testAuthTokens";

function renderProtected(initialEntry: string) {
  return render(
    <AuthSessionProvider>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/" element={<p>Public landing</p>} />
          <Route
            path="/admin"
            element={
              <ProtectedRoute allowedRoles={["SUPER_ADMIN"]}>
                <p>Super Admin Console content</p>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    </AuthSessionProvider>
  );
}

describe("ProtectedRoute (FES-07)", () => {
  afterEach(() => {
    localStorage.clear();
  });

  it("FES-07 AC: a USER-role session attempting the Super Admin Console route is redirected before it mounts", () => {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, makeFakeToken({ role: "USER" }));

    renderProtected("/admin");

    expect(screen.queryByText("Super Admin Console content")).not.toBeInTheDocument();
    expect(screen.getByText("Public landing")).toBeInTheDocument();
  });

  it("a session with no token at all is redirected the same way as a wrong-role session", () => {
    renderProtected("/admin");

    expect(screen.queryByText("Super Admin Console content")).not.toBeInTheDocument();
    expect(screen.getByText("Public landing")).toBeInTheDocument();
  });

  it("a SUPER_ADMIN-role session reaches the Super Admin Console route", () => {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, makeFakeToken({ role: "SUPER_ADMIN" }));

    renderProtected("/admin");

    expect(screen.getByText("Super Admin Console content")).toBeInTheDocument();
  });

  it("a CONSULTANT-role session is also redirected away from a SUPER_ADMIN-only route", () => {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, makeFakeToken({ role: "CONSULTANT" }));

    renderProtected("/admin");

    expect(screen.queryByText("Super Admin Console content")).not.toBeInTheDocument();
  });
});
