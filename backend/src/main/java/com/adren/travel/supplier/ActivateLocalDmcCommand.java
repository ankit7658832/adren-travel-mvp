package com.adren.travel.supplier;

/** Inputs to {@link SupplierSearchApi#activateLocalDmc} (PRD §10.3 steps 2-3, DMC-02). */
public record ActivateLocalDmcCommand(String verificationNotes) {
}
