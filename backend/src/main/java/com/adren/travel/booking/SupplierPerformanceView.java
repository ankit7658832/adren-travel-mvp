package com.adren.travel.booking;

import com.adren.travel.supplier.SupplierId;

/** HRD-11, PRD §9.5/§21.6 — the Super Admin Dashboard's per-supplier performance summary, platform scope. */
public record SupplierPerformanceView(SupplierId supplierId, long lineItemCount) {
}
