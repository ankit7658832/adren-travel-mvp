package com.adren.travel.whitelabel.event;

import com.adren.travel.whitelabel.ConsultantStatus;

import java.util.UUID;

/** Published when a Super Admin suspends or reinstates a Consultant (PRD §3.1/§21.6, FND-05). */
public record ConsultantStatusChangedEvent(UUID consultantId, ConsultantStatus newStatus) {
}
