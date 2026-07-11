/**
 * Supplier & Inventory Integration module (PRD Section 10). Aggregates
 * Hotelbeds, STUBA, TBO, Mystifly, Transferz, Widgety, HBActivities, Local
 * DMC, and BYOS sources behind one normalized search API. Public surface is
 * {@link com.adren.travel.supplier.SupplierSearchApi}.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Supplier & Inventory Integration"
)
package com.adren.travel.supplier;
