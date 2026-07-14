import { Routes, Route } from "react-router-dom";
import { SearchDashboard } from "./features/search-dashboard/SearchDashboard";
import { ConsultantStorefront } from "./features/consultant-storefront/ConsultantStorefront";

/**
 * Route scaffold matching PRD Part 21 (Screen-by-Screen UI Specification).
 * Add one route per screen as each is built:
 *   /search              -> 21.1 Search Dashboard (Layer 1)
 *   /itinerary/:id       -> 21.2 Itinerary Builder (Layer 1)
 *   /packages/new        -> 21.3 Package Builder (Layer 1)
 *   /booking/:packageId  -> 21.4 Booking & Payment Flow (mixed, doc/DESIGN.md §10)
 *   /dashboard           -> 21.5 Consultant Dashboard (Layer 1)
 *   /admin               -> 21.6 Super Admin Console (Layer 1)
 *   /wallet               -> 21.7 Wallet & Billing (Layer 1)
 *   /campaigns/new         -> 21.8 Campaign Builder (Layer 1)
 *   /pnr                   -> 21.9 PNR Search (Layer 1)
 *   /storefront             -> Layer 2 placeholder (doc/DESIGN.md §10, §12
 *                              item 4 — not a numbered Part 21 screen; PRD
 *                              gap flagged there)
 */
export default function App() {
  return (
    <Routes>
      <Route path="/" element={<SearchDashboard />} />
      <Route path="/search" element={<SearchDashboard />} />
      <Route path="/storefront" element={<ConsultantStorefront />} />
    </Routes>
  );
}
