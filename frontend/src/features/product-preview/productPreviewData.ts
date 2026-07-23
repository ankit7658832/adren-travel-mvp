/**
 * SCR-04–SCR-13 (doc/ADREN_UIUX_SPEC.md §7–§8) — PREVIEW/MOCK DATA ONLY.
 *
 * No backend endpoint exists for any of these 5 product categories beyond
 * Hotel (and even Hotel's real search — `SupplierSearchApi.searchHotels`
 * — returns only 6 normalized fields, no images/amenities/policies). Per
 * PRD §10.5, real supplier content sync (images, descriptions, amenities,
 * cancellation policies, fare rules, sailing itineraries) is explicitly
 * scheduled/production-tier work, not in this mock phase. These 10
 * screens were built anyway, as a design/prototyping reference (per
 * doc/ADREN_UIUX_SPEC.md's own "(Figma/AI-Prototyping-Ready Edition)"
 * framing) — every screen using this data renders a visible "Preview —
 * mock data" banner so it can never be mistaken for live inventory.
 */

export type ProductCategoryId = "hotel" | "flight" | "transfer" | "cruise" | "activity";

export interface ProductListItem {
  id: string;
  name: string;
  supplierBadge: string;
  price: number;
  currency: string;
  fields: { label: string; value: string }[];
}

export interface ProductDetailSection {
  heading: string;
  body: string;
}

export interface ProductDetail {
  id: string;
  name: string;
  price: number;
  currency: string;
  keyFacts: { label: string; value: string }[];
  sections: ProductDetailSection[];
}

export interface ProductCategoryConfig {
  id: ProductCategoryId;
  label: string;
  singularLabel: string;
  /** The plural URL segment under /preview/ — not a naive `${id}s` (activity -> activities). */
  routeSegment: string;
  listItems: ProductListItem[];
  details: Record<string, ProductDetail>;
}

function toDetailMap(details: ProductDetail[]): Record<string, ProductDetail> {
  return Object.fromEntries(details.map((d) => [d.id, d]));
}

const HOTEL_DETAILS: ProductDetail[] = [
  {
    id: "htl-1",
    name: "Taj Exotica Resort & Spa",
    price: 18500,
    currency: "INR",
    keyFacts: [
      { label: "Star rating", value: "5-star" },
      { label: "Meal plan", value: "Breakfast included" },
      { label: "Cancellation", value: "Free until Jul 20" },
    ],
    sections: [
      { heading: "Overview", body: "Beachfront resort set across 56 acres of tropical greenery in South Goa, with private balconies overlooking the Arabian Sea." },
      { heading: "Amenities", body: "WiFi, infinity pool, spa, fitness centre, private beach access, 3 restaurants, airport shuttle." },
      { heading: "Room Types", body: "Deluxe Garden View (INR 18,500/night), Sea View Villa (INR 27,000/night), Presidential Suite (INR 45,000/night)." },
      { heading: "Cancellation Policy", body: "Free cancellation up to 48 hours before check-in; one night charged thereafter." },
      { heading: "Location", body: "Benaulim Beach, South Goa — 35 minutes from Dabolim Airport." },
    ],
  },
  {
    id: "htl-2",
    name: "Lake Palace Udaipur",
    price: 32000,
    currency: "INR",
    keyFacts: [
      { label: "Star rating", value: "5-star" },
      { label: "Meal plan", value: "All-inclusive" },
      { label: "Cancellation", value: "Free until Aug 1" },
    ],
    sections: [
      { heading: "Overview", body: "An 18th-century palace floating on Lake Pichola, accessible only by private boat." },
      { heading: "Amenities", body: "WiFi, rooftop pool, spa, butler service, heritage tours, fine dining." },
      { heading: "Room Types", body: "Palace Room (INR 32,000/night), Historical Suite (INR 58,000/night)." },
      { heading: "Cancellation Policy", body: "Free cancellation up to 7 days before check-in." },
      { heading: "Location", body: "Lake Pichola, Udaipur — private jetty transfer from City Palace." },
    ],
  },
];

const FLIGHT_DETAILS: ProductDetail[] = [
  {
    id: "flt-1",
    name: "IndiGo 6E-204 — Delhi to Goa",
    price: 6200,
    currency: "INR",
    keyFacts: [
      { label: "Departs", value: "06:15 DEL" },
      { label: "Arrives", value: "08:55 GOI" },
      { label: "Cabin", value: "Economy" },
    ],
    sections: [
      { heading: "Itinerary", body: "Non-stop, 2h 40m. Departs Delhi (DEL) 06:15, arrives Goa (GOI) 08:55." },
      { heading: "Fare Rules", body: "Non-refundable. Change fee INR 3,000 plus fare difference." },
      { heading: "Baggage", body: "15kg check-in, 7kg cabin." },
    ],
  },
  {
    id: "flt-2",
    name: "Air India AI-865 — Mumbai to Dubai",
    price: 21500,
    currency: "INR",
    keyFacts: [
      { label: "Departs", value: "23:45 BOM" },
      { label: "Arrives", value: "02:10 DXB" },
      { label: "Cabin", value: "Business" },
    ],
    sections: [
      { heading: "Itinerary", body: "Non-stop, 3h 25m. Departs Mumbai (BOM) 23:45, arrives Dubai (DXB) 02:10 +1." },
      { heading: "Fare Rules", body: "Refundable up to 24h before departure; change fee waived." },
      { heading: "Baggage", body: "40kg check-in, 10kg cabin." },
    ],
  },
];

const TRANSFER_DETAILS: ProductDetail[] = [
  {
    id: "trf-1",
    name: "Private Sedan — Goa Airport to Benaulim",
    price: 1400,
    currency: "INR",
    keyFacts: [
      { label: "Vehicle", value: "Sedan (4 seats)" },
      { label: "Duration", value: "40 minutes" },
      { label: "Capacity", value: "3 passengers + luggage" },
    ],
    sections: [
      { heading: "Route", body: "Dabolim Airport (GOI) to Benaulim Beach — approx. 22km via NH66." },
      { heading: "Vehicle Details", body: "Air-conditioned sedan, meet-and-greet with name board at arrivals." },
      { heading: "Cancellation Deadline", body: "Free cancellation up to 6 hours before pickup." },
    ],
  },
];

const CRUISE_DETAILS: ProductDetail[] = [
  {
    id: "crs-1",
    name: "Costa Deliziosa — Mediterranean 7-Night",
    price: 145000,
    currency: "INR",
    keyFacts: [
      { label: "Sailing", value: "Sep 12–19, 2026" },
      { label: "Cabin", value: "Ocean View" },
      { label: "Ports", value: "5 ports" },
    ],
    sections: [
      { heading: "Sailing Itinerary", body: "Rome → Naples → Palermo → Barcelona → Marseille → Rome, 7 nights." },
      { heading: "Cabin Categories", body: "Interior (INR 98,000), Ocean View (INR 145,000), Balcony (INR 189,000)." },
      { heading: "Passenger Document Requirement", body: "Valid passport (6+ months validity) required for all passengers — Schengen visa may be required depending on nationality." },
    ],
  },
];

const ACTIVITY_DETAILS: ProductDetail[] = [
  {
    id: "act-1",
    name: "Sunset Dolphin Watching Cruise",
    price: 900,
    currency: "INR",
    keyFacts: [
      { label: "Duration", value: "2 hours" },
      { label: "Time slots", value: "07:00, 16:00" },
      { label: "Headcount", value: "1–8 guests" },
    ],
    sections: [
      { heading: "Description", body: "A guided boat trip along the Goan coastline to spot dolphins, with a sunset return leg." },
      { heading: "Meeting Point", body: "Sinquerim Jetty, North Goa." },
      { heading: "Inclusions", body: "Life jackets, bottled water, guide commentary." },
      { heading: "Exclusions", body: "Hotel pickup/drop, food, gratuities." },
    ],
  },
];

export const PRODUCT_CATEGORIES: Record<ProductCategoryId, ProductCategoryConfig> = {
  hotel: {
    id: "hotel",
    label: "Hotels",
    singularLabel: "Hotel",
    routeSegment: "hotels",
    details: toDetailMap(HOTEL_DETAILS),
    listItems: HOTEL_DETAILS.map((d) => ({
      id: d.id,
      name: d.name,
      supplierBadge: "Hotelbeds",
      price: d.price,
      currency: d.currency,
      fields: d.keyFacts,
    })),
  },
  flight: {
    id: "flight",
    label: "Flights",
    singularLabel: "Flight",
    routeSegment: "flights",
    details: toDetailMap(FLIGHT_DETAILS),
    listItems: FLIGHT_DETAILS.map((d) => ({
      id: d.id,
      name: d.name,
      supplierBadge: "TBO",
      price: d.price,
      currency: d.currency,
      fields: d.keyFacts,
    })),
  },
  transfer: {
    id: "transfer",
    label: "Transfers",
    singularLabel: "Transfer",
    routeSegment: "transfers",
    details: toDetailMap(TRANSFER_DETAILS),
    listItems: TRANSFER_DETAILS.map((d) => ({
      id: d.id,
      name: d.name,
      supplierBadge: "Transferz",
      price: d.price,
      currency: d.currency,
      fields: d.keyFacts,
    })),
  },
  cruise: {
    id: "cruise",
    label: "Cruises",
    singularLabel: "Cruise",
    routeSegment: "cruises",
    details: toDetailMap(CRUISE_DETAILS),
    listItems: CRUISE_DETAILS.map((d) => ({
      id: d.id,
      name: d.name,
      supplierBadge: "STUBA",
      price: d.price,
      currency: d.currency,
      fields: d.keyFacts,
    })),
  },
  activity: {
    id: "activity",
    label: "Activities",
    singularLabel: "Activity",
    routeSegment: "activities",
    details: toDetailMap(ACTIVITY_DETAILS),
    listItems: ACTIVITY_DETAILS.map((d) => ({
      id: d.id,
      name: d.name,
      supplierBadge: "HBActivities",
      price: d.price,
      currency: d.currency,
      fields: d.keyFacts,
    })),
  },
};
