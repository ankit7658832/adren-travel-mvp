package com.adren.travel.whitelabel.event;

import com.adren.travel.whitelabel.Market;

import java.util.UUID;

/** Published when a Consultant is onboarded (PRD §13.1). */
public record ConsultantOnboardedEvent(UUID consultantId, Market homeMarket) {
}
