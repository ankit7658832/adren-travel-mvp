import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { Button } from "@/shared/design-system/Button";
import { TextField } from "@/shared/design-system/TextField";
import { useForgotPassword } from "./usePasswordReset";

/**
 * SCR-00b step 1 (doc/ADREN_UIUX_SPEC.md §5.2) — "Check your email"
 * replaces the form on success, regardless of whether the email actually
 * matched an account (the backend never reveals that either — same
 * screen either way, no separate success/no-match branching here).
 */
export function ForgotPasswordScreen() {
  const [email, setEmail] = useState("");
  const forgotPassword = useForgotPassword();

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    forgotPassword.mutate(email);
  }

  if (forgotPassword.isSuccess) {
    return (
      <main className="mx-auto flex min-h-screen max-w-md flex-col justify-center px-6 py-8">
        <div role="status" className="rounded-md border border-success-700/20 bg-success-50 px-4 py-3 text-success-700">
          Check your email — if that address has an account, a reset link is on its way.
        </div>
      </main>
    );
  }

  return (
    <main className="mx-auto flex min-h-screen max-w-md flex-col justify-center px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">Forgot your password?</h1>
      <p className="mt-1 text-sm text-neutral-600">Enter your email and we&apos;ll send you a reset link.</p>

      <form onSubmit={handleSubmit} className="mt-6 space-y-4">
        <TextField
          label="Email"
          type="email"
          autoComplete="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />

        {forgotPassword.isError && (
          <p role="alert" className="text-sm text-error-700">
            Could not send the reset link. Please try again.
          </p>
        )}

        <Button type="submit" disabled={forgotPassword.isPending} className="w-full">
          {forgotPassword.isPending ? "Sending…" : "Send reset link"}
        </Button>
      </form>

      <Link to="/login" className="mt-4 text-sm text-primary-600 hover:underline">
        Back to sign in
      </Link>
    </main>
  );
}
