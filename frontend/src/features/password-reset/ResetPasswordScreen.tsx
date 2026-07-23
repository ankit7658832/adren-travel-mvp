import { useState, type FormEvent } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Button } from "@/shared/design-system/Button";
import { TextField } from "@/shared/design-system/TextField";
import { useResetPassword } from "./usePasswordReset";

/** SCR-00b step 2 — reached from the reset link's `?token=` query param. */
export function ResetPasswordScreen() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") ?? "";
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [mismatchError, setMismatchError] = useState(false);
  const resetPassword = useResetPassword();

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (newPassword !== confirmPassword) {
      setMismatchError(true);
      return;
    }
    setMismatchError(false);
    resetPassword.mutate({ token, newPassword });
  }

  if (!token) {
    return (
      <main className="mx-auto flex min-h-screen max-w-md flex-col justify-center px-6 py-8">
        <p role="alert" className="text-sm text-error-700">
          This reset link is missing its token. Please request a new one.
        </p>
        <Link to="/forgot-password" className="mt-4 text-sm text-primary-600 hover:underline">
          Request a new reset link
        </Link>
      </main>
    );
  }

  if (resetPassword.isSuccess) {
    return (
      <main className="mx-auto flex min-h-screen max-w-md flex-col justify-center px-6 py-8">
        <div role="status" className="rounded-md border border-success-700/20 bg-success-50 px-4 py-3 text-success-700">
          Your password has been reset.
        </div>
        <Link to="/login" className="mt-4 text-sm text-primary-600 hover:underline">
          Sign in with your new password
        </Link>
      </main>
    );
  }

  return (
    <main className="mx-auto flex min-h-screen max-w-md flex-col justify-center px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">Reset your password</h1>

      <form onSubmit={handleSubmit} className="mt-6 space-y-4">
        <TextField
          label="New password"
          type="password"
          autoComplete="new-password"
          required
          minLength={8}
          value={newPassword}
          onChange={(e) => {
            setNewPassword(e.target.value);
            setMismatchError(false);
          }}
        />
        <TextField
          label="Confirm new password"
          type="password"
          autoComplete="new-password"
          required
          minLength={8}
          value={confirmPassword}
          onChange={(e) => {
            setConfirmPassword(e.target.value);
            setMismatchError(false);
          }}
          error={mismatchError ? "Passwords do not match." : undefined}
        />

        {resetPassword.isError && (
          <p role="alert" className="text-sm text-error-700">
            {isInvalidToken(resetPassword.error)
              ? "This reset link is invalid or has expired."
              : "Could not reset your password. Please try again."}
          </p>
        )}

        <Button type="submit" disabled={resetPassword.isPending} className="w-full">
          {resetPassword.isPending ? "Resetting…" : "Reset password"}
        </Button>
      </form>
    </main>
  );
}

function isInvalidToken(error: unknown): boolean {
  return isAxiosErrorResponse(error) && error.response?.status === 400;
}

function isAxiosErrorResponse(error: unknown): error is { response?: { status?: number } } {
  return typeof error === "object" && error !== null && "response" in error;
}
