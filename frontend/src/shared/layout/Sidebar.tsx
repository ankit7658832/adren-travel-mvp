import { NavLink } from "react-router-dom";
import { useAuthSession } from "@/shared/auth/useAuthSession";
import { SIDEBAR_LINKS_BY_ROLE, PUBLIC_LINKS } from "./sidebarLinks";

/**
 * doc/ADREN_UIUX_SPEC.md §3 Global Navigation Shell / doc/DESIGN.md §5
 * breakpoints — persistent left sidebar at `lg`+ (icon + label),
 * collapses to an icon-only rail at `md` (text hidden via `lg:inline`,
 * not a JS breakpoint listener — matches every other responsive
 * treatment already in this codebase), hidden entirely below `md` where
 * {@link BottomTabBar} takes over navigation instead.
 */
export function Sidebar() {
  const principal = useAuthSession();
  const links = principal ? SIDEBAR_LINKS_BY_ROLE[principal.role] : PUBLIC_LINKS;

  return (
    // A <nav>, not <aside> — this genuinely is site navigation, and
    // <aside>'s implicit ARIA role ("complementary") would make it
    // invisible to a getByRole("navigation") query/assistive tech.
    <nav
      aria-label="main navigation"
      className="hidden md:flex md:w-16 lg:w-56 shrink-0 flex-col gap-1 border-r border-neutral-200 bg-surface px-2 py-4"
    >
      {links.map((link) => (
        <NavLink
          key={link.to}
          to={link.to}
          className={({ isActive }) =>
            `flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium ${
              isActive ? "bg-primary-50 text-primary-600" : "text-neutral-700 hover:bg-neutral-100"
            }`
          }
        >
          <link.icon aria-hidden="true" className="h-5 w-5 shrink-0" />
          <span className="hidden lg:inline">{link.label}</span>
        </NavLink>
      ))}
    </nav>
  );
}
