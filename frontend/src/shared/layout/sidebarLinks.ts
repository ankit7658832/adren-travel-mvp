import {
  Search,
  LayoutDashboard,
  Users,
  Wallet,
  Building2,
  KeyRound,
  Megaphone,
  Ticket,
  Bell,
  AlertTriangle,
  UserPlus,
  Truck,
  Bot,
  ClipboardCheck,
  ShieldCheck,
  Compass,
  type LucideIcon,
} from "lucide-react";
import type { Role } from "@/shared/auth/authTypes";

export interface SidebarLink {
  to: string;
  label: string;
  icon: LucideIcon;
}

// doc/ADREN_UIUX_SPEC.md §3 — the Consultant/User Shell's own list is a
// representative example (Search, Itineraries, Packages, Bookings/PNR
// Search, Wallet, Campaigns, Settings); "Itineraries"/"Packages" have no
// standalone list screen to link to (both are reached via Search ->
// Build Itinerary, or a Quotation's own "Convert to Package" step, per
// RULES.md's own "no fake/placeholder link" principle) — every OTHER
// real, already-built screen that role can reach is included instead of
// silently dropped, matching this sidebar's job (make everything
// reachable) over literal adherence to the spec's illustrative list.
export const CONSULTANT_LINKS: SidebarLink[] = [
  { to: "/search", label: "Search", icon: Search },
  { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { to: "/users", label: "Users", icon: Users },
  { to: "/wallet", label: "Wallet", icon: Wallet },
  { to: "/local-dmc", label: "Local DMC", icon: Building2 },
  { to: "/byos-credentials", label: "BYOS Credentials", icon: KeyRound },
  { to: "/campaigns/new", label: "Campaigns", icon: Megaphone },
  { to: "/pnr", label: "Bookings / PNR Search", icon: Ticket },
  { to: "/notifications", label: "Settings", icon: Bell },
  { to: "/disputes", label: "Disputes", icon: AlertTriangle },
  { to: "/preview", label: "Product Preview", icon: Compass },
];

export const SUPER_ADMIN_LINKS: SidebarLink[] = [
  { to: "/admin", label: "Admin Console", icon: ShieldCheck },
  { to: "/admin/consultants", label: "Consultants", icon: Users },
  { to: "/admin/consultants/new", label: "Onboard Consultant", icon: UserPlus },
  { to: "/admin/suppliers", label: "Suppliers", icon: Truck },
  { to: "/admin/ai-governance", label: "AI Governance Logs", icon: Bot },
  { to: "/admin/campaigns/policy-review", label: "Policy Review", icon: ClipboardCheck },
  { to: "/wallet", label: "Wallet", icon: Wallet },
  { to: "/campaigns/new", label: "Campaigns", icon: Megaphone },
  { to: "/pnr", label: "Bookings / PNR Search", icon: Ticket },
  { to: "/disputes", label: "Disputes", icon: AlertTriangle },
  { to: "/preview", label: "Product Preview", icon: Compass },
];

export const USER_LINKS: SidebarLink[] = [
  { to: "/search", label: "Search", icon: Search },
  { to: "/pnr", label: "Bookings / PNR Search", icon: Ticket },
];

export const SIDEBAR_LINKS_BY_ROLE: Record<Role, SidebarLink[]> = {
  CONSULTANT: CONSULTANT_LINKS,
  SUPER_ADMIN: SUPER_ADMIN_LINKS,
  USER: USER_LINKS,
};

export const PUBLIC_LINKS: SidebarLink[] = [{ to: "/search", label: "Search", icon: Search }];
