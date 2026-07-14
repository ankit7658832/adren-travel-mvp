package com.adren.travel.supplier.internal;

import jakarta.validation.constraints.NotBlank;

record UpdateSupplierCredentialRequest(@NotBlank String secretValue) {
}
