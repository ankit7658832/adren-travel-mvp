package com.adren.travel.supplier.internal;

import com.adren.travel.supplier.SupplierId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

record SaveByosCredentialRequest(@NotNull SupplierId supplierId, @NotBlank String secretValue) {
}
