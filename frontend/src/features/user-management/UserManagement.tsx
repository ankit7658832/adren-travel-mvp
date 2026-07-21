import { useState, type FormEvent } from "react";
import { Button } from "@/shared/design-system/Button";
import { Badge } from "@/shared/design-system/Badge";
import { useAddUser, useSetUserCapability, useUserManagement } from "./useUserManagement";

/**
 * PRD §3.3/§21.6 — a Consultant managing Users (staff/sub-agents) under
 * their own account (FND-09), including the "create package" per-Consultant
 * capability grant (PRD §6's "No (unless granted)" cell).
 */
export function UserManagement() {
  const [email, setEmail] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");
  const usersQuery = useUserManagement();
  const addUserMutation = useAddUser();
  const setCapabilityMutation = useSetUserCapability();

  function handleAddUser(e: FormEvent) {
    e.preventDefault();
    addUserMutation.mutate(
      { email, displayName, password },
      { onSuccess: () => { setEmail(""); setDisplayName(""); setPassword(""); } }
    );
  }

  return (
    <main className="mx-auto max-w-3xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">Manage Users</h1>

      <form onSubmit={handleAddUser} className="mt-6 flex flex-col gap-2 sm:flex-row sm:items-end sm:gap-3">
        <div className="flex-1">
          <label htmlFor="user-email" className="mb-1 block text-sm font-medium text-neutral-700">
            Email
          </label>
          <input
            id="user-email"
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="h-10 w-full rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-focus-ring focus-visible:ring-offset-2"
          />
        </div>
        <div className="flex-1">
          <label htmlFor="user-display-name" className="mb-1 block text-sm font-medium text-neutral-700">
            Display name
          </label>
          <input
            id="user-display-name"
            required
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            className="h-10 w-full rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-focus-ring focus-visible:ring-offset-2"
          />
        </div>
        <div className="flex-1">
          <label htmlFor="user-password" className="mb-1 block text-sm font-medium text-neutral-700">
            Initial password
          </label>
          <input
            id="user-password"
            type="password"
            required
            minLength={8}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="h-10 w-full rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-focus-ring focus-visible:ring-offset-2"
          />
        </div>
        <Button type="submit" disabled={addUserMutation.isPending}>
          {addUserMutation.isPending ? "Adding…" : "Add User"}
        </Button>
      </form>
      {addUserMutation.isError && (
        <p role="alert" className="mt-2 text-sm text-error-700">
          {addUserMutation.error instanceof Error ? addUserMutation.error.message : "Could not add user."}
        </p>
      )}

      <div className="mt-8">
        {usersQuery.isLoading && (
          <p role="status" className="text-sm text-neutral-600">
            Loading users…
          </p>
        )}

        {usersQuery.isError && (
          <div role="alert" className="flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3">
            <p className="text-sm text-error-700">Could not load users.</p>
            <Button variant="secondary" size="sm" onClick={() => usersQuery.refetch()}>
              Retry
            </Button>
          </div>
        )}

        {usersQuery.isSuccess && usersQuery.data.content.length === 0 && (
          <p className="text-sm text-neutral-600">No users yet — add your first staff member above.</p>
        )}

        {usersQuery.isSuccess && usersQuery.data.content.length > 0 && (
          <ul aria-label="user-list" className="space-y-3">
            {usersQuery.data.content.map((user) => (
              <li
                key={user.userId}
                className="flex items-center justify-between rounded-md border border-neutral-200 bg-surface px-4 py-3"
              >
                <div>
                  <p className="text-base text-neutral-900">{user.displayName}</p>
                  <p className="text-sm text-neutral-600">{user.email}</p>
                </div>
                <div className="flex items-center gap-3">
                  {user.canCreatePackage && <Badge tone="success">Can create packages</Badge>}
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() =>
                      setCapabilityMutation.mutate({ userId: user.userId, granted: !user.canCreatePackage })
                    }
                    disabled={setCapabilityMutation.isPending}
                  >
                    {user.canCreatePackage ? "Revoke package creation" : "Grant package creation"}
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </main>
  );
}
