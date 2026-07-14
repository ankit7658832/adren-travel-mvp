import { lazy, Suspense, type ReactNode } from "react";
import { Routes, Route } from "react-router-dom";
import { RouteErrorBoundary } from "./shared/components/RouteErrorBoundary";

/**
 * Route scaffold matching PRD Part 21 (Screen-by-Screen UI Specification).
 * Every screen is registered as its own React.lazy chunk (FES-01) so the app
 * never ships one monolithic bundle as the ten distinct screens land —
 * several (Super Admin Console) serve a completely different persona from
 * the Consultant/User screens and have no reason sharing an initial bundle.
 *
 *   /search              -> 21.1 Search Dashboard (Layer 1) — real
 *   /itinerary/:id       -> 21.2 Itinerary Builder (Layer 1) — FND-16
 *   /packages/new        -> 21.3 Package Builder (Layer 1) — BOK-11
 *   /booking/:packageId  -> 21.4 Booking & Payment Flow (mixed, doc/DESIGN.md §10) — BOK-13
 *   /dashboard           -> 21.5 Consultant Dashboard (Layer 1) — HRD-09
 *   /admin               -> 21.6 Super Admin Console (Layer 1) — HRD-11
 *   /wallet              -> 21.7 Wallet & Billing (Layer 1) — FIN-09
 *   /campaigns/new       -> 21.8 Campaign Builder (Layer 1) — ADS-03
 *   /pnr                 -> 21.9 PNR Search (Layer 1) — HRD-08
 *   /notifications       -> 21.10 Notification Preferences (Layer 1) — HRD-04
 *   /storefront          -> Layer 2 placeholder (doc/DESIGN.md §10, §12
 *                           item 4 — not a numbered Part 21 screen; PRD
 *                           gap flagged there)
 */
const SearchDashboard = lazy(() =>
  import("./features/search-dashboard/SearchDashboard").then((m) => ({ default: m.SearchDashboard }))
);
const ItineraryBuilder = lazy(() =>
  import("./features/itinerary-builder/ItineraryBuilder").then((m) => ({ default: m.ItineraryBuilder }))
);
const PackageBuilder = lazy(() =>
  import("./features/package-builder/PackageBuilder").then((m) => ({ default: m.PackageBuilder }))
);
const BookingPaymentFlow = lazy(() =>
  import("./features/booking-payment-flow/BookingPaymentFlow").then((m) => ({ default: m.BookingPaymentFlow }))
);
const ConsultantDashboard = lazy(() =>
  import("./features/consultant-dashboard/ConsultantDashboard").then((m) => ({ default: m.ConsultantDashboard }))
);
const SuperAdminConsole = lazy(() =>
  import("./features/super-admin-console/SuperAdminConsole").then((m) => ({ default: m.SuperAdminConsole }))
);
const WalletBilling = lazy(() =>
  import("./features/wallet-billing/WalletBilling").then((m) => ({ default: m.WalletBilling }))
);
const CampaignBuilder = lazy(() =>
  import("./features/campaign-builder/CampaignBuilder").then((m) => ({ default: m.CampaignBuilder }))
);
const PnrSearch = lazy(() =>
  import("./features/pnr-search/PnrSearch").then((m) => ({ default: m.PnrSearch }))
);
const NotificationPreferences = lazy(() =>
  import("./features/notification-preferences/NotificationPreferences").then((m) => ({
    default: m.NotificationPreferences,
  }))
);
const ConsultantStorefront = lazy(() =>
  import("./features/consultant-storefront/ConsultantStorefront").then((m) => ({ default: m.ConsultantStorefront }))
);

function RouteLoadingFallback() {
  return (
    <div role="status" className="px-6 py-8 text-sm text-neutral-600">
      Loading…
    </div>
  );
}

// One boundary instance per route (RULES.md §7.4) — a crash in one screen
// must not take down navigation or another in-progress screen, which a
// single shared boundary around <Routes> would not guarantee.
function routeElement(screen: ReactNode) {
  return <RouteErrorBoundary>{screen}</RouteErrorBoundary>;
}

export default function App() {
  return (
    <Suspense fallback={<RouteLoadingFallback />}>
      <Routes>
        <Route path="/" element={routeElement(<SearchDashboard />)} />
        <Route path="/search" element={routeElement(<SearchDashboard />)} />
        <Route path="/itinerary/:id" element={routeElement(<ItineraryBuilder />)} />
        <Route path="/packages/new" element={routeElement(<PackageBuilder />)} />
        <Route path="/booking/:packageId" element={routeElement(<BookingPaymentFlow />)} />
        <Route path="/dashboard" element={routeElement(<ConsultantDashboard />)} />
        <Route path="/admin" element={routeElement(<SuperAdminConsole />)} />
        <Route path="/wallet" element={routeElement(<WalletBilling />)} />
        <Route path="/campaigns/new" element={routeElement(<CampaignBuilder />)} />
        <Route path="/pnr" element={routeElement(<PnrSearch />)} />
        <Route path="/notifications" element={routeElement(<NotificationPreferences />)} />
        <Route path="/storefront" element={routeElement(<ConsultantStorefront />)} />
      </Routes>
    </Suspense>
  );
}
