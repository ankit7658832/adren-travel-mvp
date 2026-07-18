package com.adren.travel.supplier.internal;

import jakarta.validation.constraints.NotBlank;

record BulkUploadLocalDmcInventoryRequest(@NotBlank String csvContent) {
}
