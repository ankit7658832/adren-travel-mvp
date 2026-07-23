import { lazy, Suspense, type ReactNode } from "react";
import { Routes, Route } from "react-router-dom";
import { RouteErrorBoundary } from "./shared/components/RouteErrorBoundary";
import { ProtectedRoute } from "./shared/auth/ProtectedRoute";
import { AppShell } from "./shared/layout/AppShell";
import type { Role } from "./shared/auth/authTypes";

/**
 * Route scaffold matching PRD Part 21 (Screen-by-Screen UI Specification).
 * Every screen is registered as its own React.lazy chunk (FES-01) so the app
 * never ships one monolithic bundle as the ten distinct screens land —
 * several (Super Admin Console) serve a completely different persona from
 * the Consultant/User screens and have no reason sharing an initial bundle.
 *
 *   /login               -> Login Screen (unnumbered, HRD-14) — real
 *   /forgot-password     -> SCR-00b Forgot Password (doc/ADREN_UIUX_SPEC.md §5.2) — real
 *   /reset-password      -> SCR-00b Reset Password (doc/ADREN_UIUX_SPEC.md §5.2) — real
 *   /search              -> 21.1 Search Dashboard (Layer 1) — real
 *   /itinerary/:id       -> 21.2 Itinerary Builder (Layer 1) — FND-16
 *   /packages/new        -> 21.3 Package Builder (Layer 1) — BOK-11
 *   /booking/:packageId  -> 21.4 Booking & Payment Flow (mixed, doc/DESIGN.md §10) — BOK-13
 *   /bookings/:bookingId/confirmation -> SCR-17 Booking Confirmation (doc/ADREN_UIUX_SPEC.md §12.2) — real
 *   /dashboard           -> 21.5 Consultant Dashboard (Layer 1) — HRD-09
 *   /admin               -> 21.6 Super Admin Console (Layer 1) — HRD-11
 *   /wallet              -> 21.7 Wallet & Billing (Layer 1) — FIN-09
 *   /campaigns/new       -> 21.8 Campaign Builder (Layer 1) — ADS-03
 *   /campaigns/:id/billing -> Campaign Billing Detail (Layer 1) — ADS-11;
 *                           not a numbered Part 21 screen, same gap class
 *                           as /storefront and /disputes
 *   /pnr                 -> 21.9 PNR Search (Layer 1) — HRD-08
 *   /notifications       -> 21.10 Notification Preferences (Layer 1) — HRD-04
 *   /storefront          -> Layer 2 placeholder (doc/DESIGN.md §10, §12
 *                           item 4 — not a numbered Part 21 screen; PRD
 *                           gap flagged there)
 *   /disputes             -> Dispute Ticket Tracker (PRD §12.5) — HRD-06;
 *                           not a numbered Part 21 screen either, same
 *                           gap class as /storefront
 *
 * FES-07: every route except /, /search, and /storefront is wrapped in
 * ProtectedRoute with a per-route allowedRoles list derived from PRD §6's
 * Roles & Permissions Matrix. See protectedRouteElement's own comment for
 * why /, /search stay unguarded.
 */
const LoginScreen = lazy(() =>
  import("./features/login/LoginScreen").then((m) => ({ default: m.LoginScreen }))
);
const ForgotPasswordScreen = lazy(() =>
  import("./features/password-reset/ForgotPasswordScreen").then((m) => ({ default: m.ForgotPasswordScreen }))
);
const ResetPasswordScreen = lazy(() =>
  import("./features/password-reset/ResetPasswordScreen").then((m) => ({ default: m.ResetPasswordScreen }))
);
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
const BookingConfirmation = lazy(() =>
  import("./features/booking-confirmation/BookingConfirmation").then((m) => ({ default: m.BookingConfirmation }))
);
const ConsultantDashboard = lazy(() =>
  import("./features/consultant-dashboard/ConsultantDashboard").then((m) => ({ default: m.ConsultantDashboard }))
);
const SuperAdminConsole = lazy(() =>
  import("./features/super-admin-console/SuperAdminConsole").then((m) => ({ default: m.SuperAdminConsole }))
);
const ConsultantOnboardingWizard = lazy(() =>
  import("./features/consultant-onboarding/ConsultantOnboardingWizard").then((m) => ({
    default: m.ConsultantOnboardingWizard,
  }))
);
const ConsultantList = lazy(() =>
  import("./features/consultant-management/ConsultantList").then((m) => ({ default: m.ConsultantList }))
);
const AiGovernanceLogViewer = lazy(() =>
  import("./features/ai-governance-log/AiGovernanceLogViewer").then((m) => ({ default: m.AiGovernanceLogViewer }))
);
const LocalDmcOnboarding = lazy(() =>
  import("./features/local-dmc-onboarding/LocalDmcOnboarding").then((m) => ({ default: m.LocalDmcOnboarding }))
);
const LocalDmcBulkUpload = lazy(() =>
  import("./features/local-dmc-onboarding/LocalDmcBulkUpload").then((m) => ({ default: m.LocalDmcBulkUpload }))
);
const ByosCredentialEntry = lazy(() =>
  import("./features/byos-credential-entry/ByosCredentialEntry").then((m) => ({ default: m.ByosCredentialEntry }))
);
const UserManagement = lazy(() =>
  import("./features/user-management/UserManagement").then((m) => ({ default: m.UserManagement }))
);
const SupplierCredentialManagement = lazy(() =>
  import("./features/supplier-credentials/SupplierCredentialManagement").then((m) => ({
    default: m.SupplierCredentialManagement,
  }))
);
const WalletBilling = lazy(() =>
  import("./features/wallet-billing/WalletBilling").then((m) => ({ default: m.WalletBilling }))
);
const CampaignBuilder = lazy(() =>
  import("./features/campaign-builder/CampaignBuilder").then((m) => ({ default: m.CampaignBuilder }))
);
const PnrBookingSearch = lazy(() =>
  import("./features/pnr-search/PnrBookingSearch").then((m) => ({ default: m.PnrBookingSearch }))
);
const NotificationPreferences = lazy(() =>
  import("./features/notification-preferences/NotificationPreferences").then((m) => ({
    default: m.NotificationPreferences,
  }))
);
const ConsultantStorefront = lazy(() =>
  import("./features/consultant-storefront/ConsultantStorefront").then((m) => ({ default: m.ConsultantStorefront }))
);
const DisputeTicketTracker = lazy(() =>
  import("./features/dispute-tracker/DisputeTicketTracker").then((m) => ({ default: m.DisputeTicketTracker }))
);
const CampaignPolicyReviewQueue = lazy(() =>
  import("./features/campaign-policy-review/CampaignPolicyReviewQueue").then((m) => ({
    default: m.CampaignPolicyReviewQueue,
  }))
);
const CampaignBillingDetail = lazy(() =>
  import("./features/campaign-billing/CampaignBillingDetail").then((m) => ({ default: m.CampaignBillingDetail }))
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

// FES-07 — per-route guard, derived from PRD §6's Roles & Permissions
// Matrix (and spot-checked against each screen's actual backend
// @PreAuthorize target where the matrix's business-level wording was
// ambiguous). "/" and "/search" are deliberately left unguarded — every
// role PRD §6 defines (including no session, since no login flow exists
// yet) is allowed there, and ProtectedRoute always redirects unauthorized
// attempts *to* "/", so guarding it too would risk a redirect loop.
function protectedRouteElement(screen: ReactNode, allowedRoles: Role[]) {
  return routeElement(<ProtectedRoute allowedRoles={allowedRoles}>{screen}</ProtectedRoute>);
}

export default function App() {
  return (
    // doc/ADREN_UIUX_SPEC.md §3 Global Navigation Shell — the only
    // navigation between screens in this app; every screen used to be
    // reachable only by typing its exact URL.
    <AppShell>
      <Suspense fallback={<RouteLoadingFallback />}>
        <Routes>
          <Route path="/login" element={routeElement(<LoginScreen />)} />
          <Route path="/forgot-password" element={routeElement(<ForgotPasswordScreen />)} />
          <Route path="/reset-password" element={routeElement(<ResetPasswordScreen />)} />
          <Route path="/" element={routeElement(<SearchDashboard />)} />
          <Route path="/search" element={routeElement(<SearchDashboard />)} />
          <Route
            path="/itinerary/:id"
            element={protectedRouteElement(<ItineraryBuilder />, ["SUPER_ADMIN", "CONSULTANT", "USER"])}
          />
          <Route
            path="/packages/new"
            element={protectedRouteElement(<PackageBuilder />, ["SUPER_ADMIN", "CONSULTANT"])}
          />
          <Route
            path="/booking/:packageId"
            element={protectedRouteElement(<BookingPaymentFlow />, ["SUPER_ADMIN", "CONSULTANT", "USER"])}
          />
          <Route
            path="/bookings/:bookingId/confirmation"
            element={protectedRouteElement(<BookingConfirmation />, ["SUPER_ADMIN", "CONSULTANT", "USER"])}
          />
          <Route path="/dashboard" element={protectedRouteElement(<ConsultantDashboard />, ["CONSULTANT"])} />
          <Route path="/admin" element={protectedRouteElement(<SuperAdminConsole />, ["SUPER_ADMIN"])} />
          <Route
            path="/admin/consultants/new"
            element={protectedRouteElement(<ConsultantOnboardingWizard />, ["SUPER_ADMIN"])}
          />
          <Route path="/admin/consultants" element={protectedRouteElement(<ConsultantList />, ["SUPER_ADMIN"])} />
          <Route path="/users" element={protectedRouteElement(<UserManagement />, ["CONSULTANT"])} />
          <Route
            path="/admin/suppliers"
            element={protectedRouteElement(<SupplierCredentialManagement />, ["SUPER_ADMIN"])}
          />
          <Route
            path="/admin/ai-governance"
            element={protectedRouteElement(<AiGovernanceLogViewer />, ["SUPER_ADMIN"])}
          />
          <Route
            path="/admin/campaigns/policy-review"
            element={protectedRouteElement(<CampaignPolicyReviewQueue />, ["SUPER_ADMIN"])}
          />
          <Route path="/local-dmc" element={protectedRouteElement(<LocalDmcOnboarding />, ["CONSULTANT"])} />
          <Route
            path="/local-dmc/:id/inventory"
            element={protectedRouteElement(<LocalDmcBulkUpload />, ["CONSULTANT"])}
          />
          <Route path="/byos-credentials" element={protectedRouteElement(<ByosCredentialEntry />, ["CONSULTANT"])} />
          <Route path="/wallet" element={protectedRouteElement(<WalletBilling />, ["SUPER_ADMIN", "CONSULTANT"])} />
          <Route
            path="/campaigns/new"
            element={protectedRouteElement(<CampaignBuilder />, ["SUPER_ADMIN", "CONSULTANT"])}
          />
          <Route
            path="/campaigns/:campaignId/billing"
            element={protectedRouteElement(<CampaignBillingDetail />, ["SUPER_ADMIN", "CONSULTANT"])}
          />
          <Route
            path="/pnr"
            element={protectedRouteElement(<PnrBookingSearch />, ["SUPER_ADMIN", "CONSULTANT", "USER"])}
          />
          <Route
            path="/notifications"
            element={protectedRouteElement(<NotificationPreferences />, ["CONSULTANT"])}
          />
          {/* Layer 2, End Traveler-facing — no internal role required. */}
          <Route path="/storefront" element={routeElement(<ConsultantStorefront />)} />
          <Route
            path="/disputes"
            element={protectedRouteElement(<DisputeTicketTracker />, ["SUPER_ADMIN", "CONSULTANT"])}
          />
        </Routes>
      </Suspense>
    </AppShell>
  );
}
