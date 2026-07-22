import type { ReactNode } from "react";
import { Sidebar } from "./Sidebar";
import { TopBar } from "./TopBar";
import { BottomTabBar } from "./BottomTabBar";

/**
 * doc/ADREN_UIUX_SPEC.md §3 Global Navigation Shell — composes the three
 * responsive pieces (doc/DESIGN.md §5): a persistent {@link Sidebar} at
 * `md`+ (icon rail at `md`, full at `lg`+), a {@link TopBar} always
 * visible, and a {@link BottomTabBar} that replaces the sidebar below
 * `md`. Wraps every route (mounted once in App.tsx, outside `<Routes>`),
 * same "every screen reachable, not just the ones someone remembered to
 * link to" reasoning the single-bar NavBar this replaces was built for.
 */
export function AppShell({ children }: { children: ReactNode }) {
  return (
    <div className="flex h-screen flex-col">
      <TopBar />
      <div className="flex min-h-0 flex-1">
        <Sidebar />
        {/* A plain div, not <main> — every screen this wraps already
            renders its own top-level <main>; nesting a second one here
            would produce an invalid, ambiguous "main" landmark. */}
        <div className="min-w-0 flex-1 overflow-y-auto pb-14 md:pb-0">{children}</div>
      </div>
      <BottomTabBar />
    </div>
  );
}
