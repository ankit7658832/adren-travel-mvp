import { NavLink } from "react-router-dom";
import { Search, LayoutDashboard, ShieldCheck, Ticket } from "lucide-react";
import { useAuthSession } from "@/shared/auth/useAuthSession";
import type { Role } from "@/shared/auth/authTypes";

const PRIMARY_TAB_BY_ROLE: Record<Role, { to: string; label: string; icon: typeof LayoutDashboard }> = {
  CONSULTANT: { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  SUPER_ADMIN: { to: "/admin", label: "Admin", icon: ShieldCheck },
  USER: { to: "/pnr", label: "Bookings", icon: Ticket },
};

/**
 * doc/DESIGN.md §5 — below `md` the sidebar disappears entirely and this
 * fixed bottom bar takes over, per §5's explicit "reasonable, not equally
 * optimized" degradation target for Layer 1: a curated subset (Search
 * plus one role-primary destination), not every {@link Sidebar} link —
 * there isn't room, and this is the secondary surface for this
 * desktop-first operations tool.
 */
export function BottomTabBar() {
  const principal = useAuthSession();
  const primaryTab = principal ? PRIMARY_TAB_BY_ROLE[principal.role] : null;

  const tabClass = ({ isActive }: { isActive: boolean }) =>
    `flex flex-1 flex-col items-center gap-1 py-2 text-xs font-medium ${
      isActive ? "text-primary-600" : "text-neutral-600"
    }`;

  return (
    <nav
      aria-label="mobile navigation"
      className="fixed inset-x-0 bottom-0 z-10 flex border-t border-neutral-200 bg-surface md:hidden"
    >
      <NavLink to="/search" className={tabClass}>
        <Search aria-hidden="true" className="h-5 w-5" />
        Search
      </NavLink>
      {primaryTab && (
        <NavLink to={primaryTab.to} className={tabClass}>
          <primaryTab.icon aria-hidden="true" className="h-5 w-5" />
          {primaryTab.label}
        </NavLink>
      )}
    </nav>
  );
}
