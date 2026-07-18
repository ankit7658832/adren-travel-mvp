package com.adren.travel.supplier;

import java.util.List;

/**
 * Result of a Local DMC bulk-inventory CSV upload (PRD §10.2.8, DMC-03) —
 * all-or-nothing: {@code errors} non-empty means ZERO rows were persisted
 * (no partial silent import), per the story's own AC.
 */
public record LocalDmcInventoryUploadResult(int successCount, List<LocalDmcInventoryRowError> errors) {
}
