package com.adren.travel.supplier.event;

import java.util.UUID;

/** Published whenever a Local DMC inventory item's rate/details are edited after initial upload (PRD §10.2.8, DMC-10). */
public record LocalDmcInventoryItemUpdatedEvent(UUID itemId, UUID localDmcId, UUID consultantId) {
}
