package com.adren.travel.supplier;

import java.util.List;

/** One invalid CSV row from {@link SupplierSearchApi#bulkUploadLocalDmcInventory} (PRD §10.2.8, DMC-03). {@code rowNumber} is 1-based, header excluded. */
public record LocalDmcInventoryRowError(int rowNumber, List<String> fieldErrors) {
}
