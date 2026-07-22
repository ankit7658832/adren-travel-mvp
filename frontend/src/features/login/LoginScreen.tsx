import { useState, type FormEvent } from "react";
import { Button } from "@/shared/design-system/Button";
import { TextField } from "@/shared/design-system/TextField";
import { useLogin, landingRouteFor } from "./useLogin";

/**
 * HRD-14 — the app's first real login screen (PRD §6/§13.1), replacing the
 * complete absence of one every prior stage worked around via
 * `DevAuthController`'s local-dev-only token shortcut. A hard navigation
 * (`window.location.href`) on success, not client-side routing: `useMemo`
 * inside `AuthSessionProvider` derives the session principal once, at
 * mount, from `localStorage` — only a fresh page load re-derives it.
 */
export function LoginScreen() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const loginMutation = useLogin();

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    loginMutation.mutate(
      { email, password },
      {
        onSuccess: (data) => {
          window.location.href = landingRouteFor(data.role);
        },
      }
    );
  }

  return (
    <main className="mx-auto flex min-h-screen max-w-md flex-col justify-center px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">Sign in</h1>

      <form onSubmit={handleSubmit} className="mt-6 space-y-4">
        <TextField
          label="Email"
          type="email"
          autoComplete="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <TextField
          label="Password"
          type="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        {loginMutation.isError && (
          <p role="alert" className="text-sm text-error-700">
            {isInvalidCredentials(loginMutation.error) ? "Invalid email or password." : "Could not sign in. Please try again."}
          </p>
        )}

        <Button type="submit" disabled={loginMutation.isPending} className="w-full">
          {loginMutation.isPending ? "Signing in…" : "Sign in"}
        </Button>
      </form>
    </main>
  );
}

function isInvalidCredentials(error: unknown): boolean {
  return isAxiosErrorResponse(error) && error.response?.status === 401;
}

function isAxiosErrorResponse(error: unknown): error is { response?: { status?: number } } {
  return typeof error === "object" && error !== null && "response" in error;
}
