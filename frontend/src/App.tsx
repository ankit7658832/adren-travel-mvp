import { Routes, Route } from "react-router-dom";
import { SearchDashboard } from "./features/search-dashboard/SearchDashboard";

/**
 * Route scaffold matching PRD Part 21 (Screen-by-Screen UI Specification).
 * Add one route per screen as each is built:
 *   /search              -> 21.1 Search Dashboard
 *   /itinerary/:id       -> 21.2 Itinerary Builder
 *   /packages/new        -> 21.3 Package Builder
 *   /booking/:packageId  -> 21.4 Booking & Payment Flow
 *   /dashboard           -> 21.5 Consultant Dashboard
 *   /admin               -> 21.6 Super Admin Console
 *   /wallet               -> 21.7 Wallet & Billing
 *   /campaigns/new         -> 21.8 Campaign Builder
 *   /pnr                   -> 21.9 PNR Search
 */
export default function App() {
  return (
    <Routes>
      <Route path="/" element={<SearchDashboard />} />
      <Route path="/search" element={<SearchDashboard />} />
    </Routes>
  );
}
