import { Link } from "react-router-dom";
import { useAuthSession } from "@/shared/auth/useAuthSession";
import { AUTH_TOKEN_STORAGE_KEY, type Role } from "@/shared/auth/authTypes";

interface NavLink {
  to: string;
  label: string;
}

const CONSULTANT_LINKS: NavLink[] = [
  { to: "/dashboard", label: "Dashboard" },
  { to: "/users", label: "Users" },
  { to: "/wallet", label: "Wallet" },
  { to: "/local-dmc", label: "Local DMC" },
  { to: "/byos-credentials", label: "BYOS Credentials" },
  { to: "/campaigns/new", label: "New Campaign" },
  { to: "/pnr", label: "PNR Search" },
  { to: "/notifications", label: "Notifications" },
  { to: "/disputes", label: "Disputes" },
];

const SUPER_ADMIN_LINKS: NavLink[] = [
  { to: "/admin", label: "Admin Console" },
  { to: "/admin/consultants/new", label: "Onboard Consultant" },
  { to: "/admin/consultants", label: "Consultants" },
  { to: "/admin/suppliers", label: "Suppliers" },
  { to: "/admin/ai-governance", label: "AI Governance" },
  { to: "/admin/campaigns/policy-review", label: "Policy Review" },
  { to: "/wallet", label: "Wallet" },
  { to: "/campaigns/new", label: "New Campaign" },
  { to: "/pnr", label: "PNR Search" },
  { to: "/disputes", label: "Disputes" },
];

const USER_LINKS: NavLink[] = [{ to: "/pnr", label: "PNR Search" }];

const ROLE_LINKS: Record<Role, NavLink[]> = {
  CONSULTANT: CONSULTANT_LINKS,
  SUPER_ADMIN: SUPER_ADMIN_LINKS,
  USER: USER_LINKS,
};

/**
 * HRD-14 follow-up — every screen in this app was previously reachable
 * only by typing its exact URL: no shared nav/header existed anywhere,
 * so a real login had nowhere real to take a signed-in user (PRD §6's
 * role matrix defines WHAT each role can reach, but nothing ever
 * rendered it as actual links). Role-derived from `useAuthSession`, the
 * same session HRD-14's login screen and every `ProtectedRoute` already
 * read — no separate link-visibility logic to keep in sync.
 */
export function NavBar() {
  const principal = useAuthSession();
  const links = principal ? ROLE_LINKS[principal.role] : [];

  function handleSignOut() {
    window.localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
    window.location.href = "/login";
  }

  return (
    <nav
      aria-label="main navigation"
      className="flex flex-wrap items-center gap-x-4 gap-y-2 border-b border-neutral-200 bg-surface px-6 py-3"
    >
      <Link to="/search" className="text-sm font-semibold text-neutral-900">
        Adren
      </Link>
      <Link to="/search" className="text-sm text-neutral-700 hover:text-primary-600">
        Search
      </Link>
      {links.map((link) => (
        <Link key={link.to} to={link.to} className="text-sm text-neutral-700 hover:text-primary-600">
          {link.label}
        </Link>
      ))}
      <span className="flex-1" />
      {principal ? (
        <button
          type="button"
          onClick={handleSignOut}
          className="text-sm font-medium text-neutral-700 hover:text-primary-600"
        >
          Sign out
        </button>
      ) : (
        <Link to="/login" className="text-sm font-medium text-primary-600 hover:underline">
          Sign in
        </Link>
      )}
    </nav>
  );
}
